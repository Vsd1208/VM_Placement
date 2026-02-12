import org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;

import java.util.Comparator;
import java.util.Optional;

public class EnergyVmAllocationPolicy extends VmAllocationPolicySimple {

    @Override
    protected Optional<Host> defaultFindHostForVm(final Vm vm) {

        return getHostList().stream()
                .filter(host -> host.isSuitableForVm(vm))
                .min(Comparator.comparingDouble(host -> power(host)));
    }

    private double power(final Host host) {

        // Safe CPU utilization calculation (works for all versions)
        double utilization = 0.0;
        if (host.getTotalMipsCapacity() > 0) {
            utilization = host.getCpuMipsUtilization() / host.getTotalMipsCapacity();
        }

        // Linear power model
        return 175 + (250 - 175) * utilization;
    }
}
