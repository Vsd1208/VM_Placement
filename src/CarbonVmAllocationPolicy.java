import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.Comparator;

public class CarbonVmAllocationPolicy extends VmAllocationPolicySimple {

    double alpha = 0.4;
    double beta = 0.3;
    double gamma = 0.3;

    @Override
    public Host findHostForVm(Vm vm) {

        return getHostList().stream()
                .filter(host -> host.isSuitableForVm(vm))
                .min(Comparator.comparingDouble(this::score))
                .orElse(null);
    }

    private double score(Host host) {

        double utilization = host.getCpuPercentUtilization();
        double power = 175 + (250 - 175) * utilization;

        double carbon = (double) host.getAttributes()
                .getOrDefault("carbon", 500.0);

        return alpha * utilization
                + beta * (power / 250)
                + gamma * (carbon / 700);
    }
}
