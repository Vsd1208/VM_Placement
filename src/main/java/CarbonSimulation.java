import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSim;
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
import java.util.List;

public class CarbonSimulation {

    public static void main(String[] args) {

        // Initialize simulation
        CloudSim simulation = new CloudSim(1);

        // Broker
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        // Carbon-aware policy
        CarbonVmAllocationPolicy policy = new CarbonVmAllocationPolicy();

        // Datacenter
        Datacenter datacenter = new DatacenterSimple(simulation, createHosts(50), policy);

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
}
