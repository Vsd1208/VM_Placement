import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicy;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.provisioners.PeProvisionerSimple;
import org.cloudsimplus.provisioners.ResourceProvisionerSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CarbonSimulation {

    public static void main(String[] args) {
        final int runs = getIntEnv("EVAL_RUNS", 5);
        final int hostCount = getIntEnv("HOST_COUNT", 50);
        final int vmCount = getIntEnv("VM_COUNT", 100);
        final int cloudletCount = getIntEnv("CLOUDLET_COUNT", 100);

        final List<String> zones = resolveZones();
        final CarbonIntensityProvider carbonIntensityProvider = new RealTimeCarbonIntensityProvider();
        final List<ResultsLogger.EvaluationMetrics> allMetrics = new ArrayList<>();

        for (int run = 1; run <= runs; run++) {
            allMetrics.add(runSingleExperiment(
                    run,
                    "FIRST_FIT",
                    hostCount,
                    vmCount,
                    cloudletCount,
                    zones,
                    carbonIntensityProvider
            ));
            allMetrics.add(runSingleExperiment(
                    run,
                    "ENERGY_AWARE",
                    hostCount,
                    vmCount,
                    cloudletCount,
                    zones,
                    carbonIntensityProvider
            ));
            allMetrics.add(runSingleExperiment(
                    run,
                    "CIAVMP",
                    hostCount,
                    vmCount,
                    cloudletCount,
                    zones,
                    carbonIntensityProvider
            ));
        }

        ResultsLogger.writeResearchOutputs(
                allMetrics,
                runs,
                hostCount,
                vmCount,
                cloudletCount
        );

        System.out.println("Raw metrics written to: "
                + Path.of("results", "evaluation_raw_metrics.csv").toAbsolutePath());
        System.out.println("Policy summary written to: "
                + Path.of("results", "evaluation_policy_summary.csv").toAbsolutePath());
        System.out.println("Research report written to: "
                + Path.of("results", "evaluation_research_summary.txt").toAbsolutePath());
    }

    private static ResultsLogger.EvaluationMetrics runSingleExperiment(
            final int runId,
            final String policyName,
            final int hostCount,
            final int vmCount,
            final int cloudletCount,
            final List<String> zones,
            final CarbonIntensityProvider carbonIntensityProvider) {

        final CloudSimPlus simulation = new CloudSimPlus();
        final DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        final List<Host> hosts = createHosts(hostCount);
        final Map<Host, String> hostRegionMap = createHostRegionMap(hosts, zones);
        final VmAllocationPolicy policy = createPolicy(
                policyName,
                carbonIntensityProvider,
                hostRegionMap
        );

        new DatacenterSimple(simulation, hosts, policy);

        final List<Vm> vmList = createVMs(vmCount);
        final List<Cloudlet> cloudletList = createCloudlets(cloudletCount);

        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);
        bindCloudletsToVms(broker, cloudletList, vmList);

        simulation.start();
        final long finishedCloudlets = broker.getCloudletFinishedList().size();
        System.out.printf(
                "Run %d | %s | finished cloudlets: %d%n",
                runId,
                policyName,
                finishedCloudlets
        );

        return ResultsLogger.buildMetrics(
                runId,
                policyName,
                cloudletList,
                hosts,
                hostRegionMap,
                carbonIntensityProvider
        );
    }

    private static VmAllocationPolicy createPolicy(
            final String policyName,
            final CarbonIntensityProvider carbonIntensityProvider,
            final Map<Host, String> hostRegionMap) {
        switch (policyName) {
            case "FIRST_FIT":
                return new FirstFitVmAllocationPolicy();
            case "ENERGY_AWARE":
                return new EnergyVmAllocationPolicy();
            case "CIAVMP":
                return new CarbonVmAllocationPolicy(carbonIntensityProvider, hostRegionMap);
            default:
                throw new IllegalArgumentException("Unsupported policy: " + policyName);
        }
    }

    private static void bindCloudletsToVms(
            final DatacenterBroker broker,
            final List<Cloudlet> cloudletList,
            final List<Vm> vmList) {
        for (int i = 0; i < cloudletList.size(); i++) {
            broker.bindCloudletToVm(
                    cloudletList.get(i),
                    vmList.get(i % vmList.size())
            );
        }
    }

    private static List<Host> createHosts(int number) {
        List<Host> hostList = new ArrayList<>();

        for (int i = 0; i < number; i++) {

            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                peList.add(new PeSimple(1000, new PeProvisionerSimple()));
            }

            HostSimple host = new HostSimple(
                    16000, // RAM (MB)
                    10000, // Bandwidth
                    1000000, // Storage
                    peList);

            host.setRamProvisioner(new ResourceProvisionerSimple());
            host.setBwProvisioner(new ResourceProvisionerSimple());
            host.setVmScheduler(new VmSchedulerTimeShared());

            hostList.add(host);
        }

        return hostList;
    }

    private static List<Vm> createVMs(int number) {
        List<Vm> vmList = new ArrayList<>();

        for (int i = 0; i < number; i++) {
            Vm vm = new VmSimple(1000, 1);

            vm.setRam(1024)
                    .setBw(1000)
                    .setSize(10000);

            vmList.add(vm);
        }

        return vmList;
    }

    private static List<Cloudlet> createCloudlets(int number) {
        List<Cloudlet> cloudletList = new ArrayList<>();

        UtilizationModelDynamic utilization = new UtilizationModelDynamic(0.7);

        for (int i = 0; i < number; i++) {
            Cloudlet cloudlet = new CloudletSimple(
                    10000, // length
                    1, // PEs
                    utilization);

            cloudletList.add(cloudlet);
        }

        return cloudletList;
    }

    private static Map<Host, String> createHostRegionMap(
            final List<Host> hostList,
            final List<String> zones) {
        final Map<Host, String> hostRegionMap = new IdentityHashMap<>();

        for (int i = 0; i < hostList.size(); i++) {
            hostRegionMap.put(hostList.get(i), zones.get(i % zones.size()));
        }

        return hostRegionMap;
    }

    private static List<String> resolveZones() {
        final String configuredZones = System.getenv("CARBON_ZONES");
        if (configuredZones == null || configuredZones.isBlank()) {
            return List.of(
                    "US-CAL-CISO",
                    "US-MIDA-PJM",
                    "US-TEX-ERCO",
                    "US-NY-NYIS"
            );
        }

        final List<String> parsedZones = Arrays.stream(configuredZones.split(","))
                .map(String::trim)
                .filter(zone -> !zone.isEmpty())
                .collect(Collectors.toList());

        if (parsedZones.isEmpty()) {
            return List.of("US-CAL-CISO");
        }

        return parsedZones;
    }

    private static int getIntEnv(final String envVar, final int defaultValue) {
        final String value = System.getenv(envVar);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            final int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
