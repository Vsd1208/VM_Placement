import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;

import java.util.*;

public class CarbonSimulation {

    public static void main(String[] args) {

        CloudSim simulation = new CloudSim();
        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);

        // Choose policy here
        CarbonVmAllocationPolicy policy = new CarbonVmAllocationPolicy();

        DatacenterSimple datacenter = new DatacenterSimple(simulation, createHosts(50), policy);

        broker.submitVmList(createVMs(100));
        broker.submitCloudletList(createCloudlets(100));

        simulation.start();

        System.out.println("Simulation completed.");
    }

    private static List<HostSimple> createHosts(int number) {
        List<HostSimple> list = new ArrayList<>();
        Random rand = new Random();

        for (int i = 0; i < number; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                peList.add(new PeSimple(1000));
            }

            HostSimple host = new HostSimple(
                    16000,
                    10000,
                    1000000,
                    peList);

            // Store carbon intensity as attribute
            host.getAttributes().put("carbon",
                    300 + rand.nextDouble() * 400);

            list.add(host);
        }
        return list;
    }

    private static List<VmSimple> createVMs(int number) {
        List<VmSimple> list = new ArrayList<>();

        for (int i = 0; i < number; i++) {
            VmSimple vm = new VmSimple(1000, 1);
            vm.setRam(1024).setBw(1000).setSize(10000);
            list.add(vm);
        }
        return list;
    }

    private static List<CloudletSimple> createCloudlets(int number) {
        List<CloudletSimple> list = new ArrayList<>();

        for (int i = 0; i < number; i++) {
            list.add(new CloudletSimple(
                    10000,
                    1,
                    new UtilizationModelDynamic(0.7)));
        }
        return list;
    }
}
