import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CarbonSimulation {

    public static void main(String[] args) {

        // Initialize simulation
        CloudSimPlus simulation = new CloudSimPlus();

        // Broker
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        List<Host> hosts = createHosts(50);
        Map<Host, String> hostRegionMap = createHostRegionMap(hosts);

        // Carbon-aware policy
        CarbonIntensityProvider carbonIntensityProvider = new RealTimeCarbonIntensityProvider();
        CarbonVmAllocationPolicy policy =
                new CarbonVmAllocationPolicy(carbonIntensityProvider, hostRegionMap);

        // Datacenter
        Datacenter datacenter = new DatacenterSimple(simulation, hosts, policy);

        // Create VMs and Cloudlets
        List<Vm> vmList = createVMs(100);
        List<Cloudlet> cloudletList = createCloudlets(100);

        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);

        // Bind Cloudlets to VMs
        for (int i = 0; i < cloudletList.size(); i++) {
            broker.bindCloudletToVm(
                    cloudletList.get(i),
                    vmList.get(i % vmList.size()));
        }

        // Start simulation
        simulation.start();

        System.out.println("Simulation completed.");
        System.out.println("Finished Cloudlets: "
                + broker.getCloudletFinishedList().size());
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

    private static Map<Host, String> createHostRegionMap(final List<Host> hostList) {
        final List<String> zones = resolveZones();
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
}
