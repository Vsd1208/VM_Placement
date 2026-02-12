import org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;

import java.util.Comparator;
import java.util.Optional;

public class CarbonVmAllocationPolicy extends VmAllocationPolicySimple {

    private final double alpha = 0.4;
    private final double beta = 0.3;
    private final double gamma = 0.3;

    @Override
    protected Optional<Host> defaultFindHostForVm(final Vm vm) {
        return getHostList().stream()
                .filter(host -> host.isSuitableForVm(vm))
                .min(Comparator.comparingDouble(this::score));
    }

    private double score(Host host) {

        // CPU utilization (safe for all versions)
        double utilization = 0.0;
        if (host.getTotalMipsCapacity() > 0) {
            utilization = host.getCpuMipsUtilization() / host.getTotalMipsCapacity();
        }

        // Power model (Watts)
        double power = 175 + (250 - 175) * utilization;

        // Constant carbon value (since attributes not supported)
        double carbon = 500.0;

        // Weighted score (lower is better)
        return alpha * utilization
                + beta * (power / 250.0)
                + gamma * (carbon / 700.0);
    }
}
