import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.Comparator;

public class EnergyVmAllocationPolicy extends VmAllocationPolicySimple {

    @Override
    public Host findHostForVm(Vm vm) {
        return getHostList().stream()
                .filter(host -> host.isSuitableForVm(vm))
                .min(Comparator.comparingDouble(this::power))
                .orElse(null);
    }

    private double power(Host host) {
        double utilization = host.getCpuPercentUtilization();
        return 175 + (250 - 175) * utilization;
    }
}
