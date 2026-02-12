import org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class CarbonVmAllocationPolicy extends VmAllocationPolicySimple {

    private final double alpha = 0.4;
    private final double beta = 0.3;
    private final double gamma = 0.3;
    private final CarbonIntensityProvider carbonIntensityProvider;
    private final Map<Host, String> hostRegionMap;
    private final String defaultRegion;

    public CarbonVmAllocationPolicy() {
        this(new RealTimeCarbonIntensityProvider(), Map.of());
    }

    public CarbonVmAllocationPolicy(
            final CarbonIntensityProvider carbonIntensityProvider,
            final Map<Host, String> hostRegionMap) {
        this.carbonIntensityProvider = carbonIntensityProvider;
        this.hostRegionMap = hostRegionMap;
        this.defaultRegion = "US-CAL-CISO";
    }

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

        // Region-aware carbon intensity (gCO2/kWh)
        double carbon = carbonIntensityProvider.getIntensityGco2PerKwh(resolveRegion(host));

        // Weighted score (lower is better)
        return alpha * utilization
                + beta * (power / 250.0)
                + gamma * (carbon / 700.0);
    }

    private String resolveRegion(final Host host) {
        return hostRegionMap.getOrDefault(host, defaultRegion);
    }
}
