import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class ResultsLogger {

    private static final double IDLE_POWER_WATTS = 175.0;
    private static final double MAX_POWER_WATTS = 250.0;
    private static final String DEFAULT_REGION = "US-CAL-CISO";

    private ResultsLogger() {
    }

    public static EvaluationMetrics buildMetrics(
            final int runId,
            final String policyName,
            final List<Cloudlet> cloudlets,
            final List<Host> hosts,
            final Map<Host, String> hostRegionMap,
            final CarbonIntensityProvider carbonIntensityProvider) {
        final int totalCloudlets = cloudlets.size();
        final long finishedCloudlets = cloudlets.stream()
                .filter(Cloudlet::isFinished)
                .count();

        final double completionRate = totalCloudlets == 0
                ? 0.0
                : (finishedCloudlets * 100.0) / totalCloudlets;

        final double avgCpuTime = cloudlets.stream()
                .filter(Cloudlet::isFinished)
                .mapToDouble(Cloudlet::getActualCpuTime)
                .average()
                .orElse(0.0);

        final double makespan = cloudlets.stream()
                .filter(Cloudlet::isFinished)
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0.0);

        final HostEnergyAndCarbon totals = estimateEnergyAndCarbon(
                cloudlets,
                hosts,
                makespan,
                hostRegionMap,
                carbonIntensityProvider
        );

        return new EvaluationMetrics(
                Instant.now().toString(),
                runId,
                policyName,
                totalCloudlets,
                finishedCloudlets,
                completionRate,
                avgCpuTime,
                makespan,
                totals.energyKwh,
                totals.carbonKg
        );
    }

    public static void writeResearchOutputs(
            final List<EvaluationMetrics> metrics,
            final int runs,
            final int hostCount,
            final int vmCount,
            final int cloudletCount) {
        final Path resultsDir = Path.of("results");
        final Path rawCsvPath = resultsDir.resolve("evaluation_raw_metrics.csv");
        final Path summaryCsvPath = resultsDir.resolve("evaluation_policy_summary.csv");
        final Path researchSummaryPath = resultsDir.resolve("evaluation_research_summary.txt");

        final Map<String, List<EvaluationMetrics>> byPolicy = metrics.stream()
                .collect(Collectors.groupingBy(
                        EvaluationMetrics::policyName,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        final Map<String, PolicyStats> policyStats = new LinkedHashMap<>();
        for (Map.Entry<String, List<EvaluationMetrics>> entry : byPolicy.entrySet()) {
            policyStats.put(entry.getKey(), PolicyStats.from(entry.getValue()));
        }

        final List<String> rawCsvLines = new ArrayList<>();
        rawCsvLines.add("timestamp,run_id,policy,total_cloudlets,finished_cloudlets,completion_rate_pct,"
                + "avg_cpu_time_s,makespan_s,energy_kwh,carbon_kg_co2");
        for (EvaluationMetrics metric : metrics) {
            rawCsvLines.add(metric.toCsvLine());
        }

        final List<String> summaryCsvLines = new ArrayList<>();
        summaryCsvLines.add("policy,runs,completion_mean_pct,completion_std_pct,makespan_mean_s,makespan_std_s,"
                + "energy_mean_kwh,energy_std_kwh,carbon_mean_kg,carbon_std_kg");
        for (Map.Entry<String, PolicyStats> entry : policyStats.entrySet()) {
            summaryCsvLines.add(entry.getValue().toCsvLine(entry.getKey()));
        }

        final String report = buildResearchReport(
                policyStats,
                runs,
                hostCount,
                vmCount,
                cloudletCount
        );

        try {
            Files.createDirectories(resultsDir);
            Files.write(rawCsvPath, rawCsvLines, StandardCharsets.UTF_8);
            Files.write(summaryCsvPath, summaryCsvLines, StandardCharsets.UTF_8);
            Files.writeString(researchSummaryPath, report, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write evaluation files under results.", e);
        }
    }

    private static String buildResearchReport(
            final Map<String, PolicyStats> statsByPolicy,
            final int runs,
            final int hostCount,
            final int vmCount,
            final int cloudletCount) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Research Evaluation Summary").append(System.lineSeparator());
        sb.append("===========================").append(System.lineSeparator());
        sb.append(String.format(
                Locale.US,
                "Generated At: %s%nRuns Per Policy: %d%nHosts: %d%nVMs: %d%nCloudlets: %d%n%n",
                Instant.now(),
                runs,
                hostCount,
                vmCount,
                cloudletCount
        ));

        sb.append("Policy Statistics (mean +- std)").append(System.lineSeparator());
        sb.append("--------------------------------").append(System.lineSeparator());
        for (Map.Entry<String, PolicyStats> entry : statsByPolicy.entrySet()) {
            final PolicyStats s = entry.getValue();
            sb.append(String.format(
                    Locale.US,
                    "%s | completion: %.2f +- %.2f %% | makespan: %.2f +- %.2f s | energy: %.4f +- %.4f kWh | carbon: %.4f +- %.4f kg CO2%n",
                    entry.getKey(),
                    s.completionMean,
                    s.completionStd,
                    s.makespanMean,
                    s.makespanStd,
                    s.energyMean,
                    s.energyStd,
                    s.carbonMean,
                    s.carbonStd
            ));
        }

        final PolicyStats ciavmp = statsByPolicy.get("CIAVMP");
        if (ciavmp != null) {
            sb.append(System.lineSeparator());
            sb.append("CIAVMP Relative Improvements").append(System.lineSeparator());
            sb.append("-----------------------------").append(System.lineSeparator());
            appendImprovementLine(sb, "FIRST_FIT", statsByPolicy.get("FIRST_FIT"), ciavmp);
            appendImprovementLine(sb, "ENERGY_AWARE", statsByPolicy.get("ENERGY_AWARE"), ciavmp);
        }

        return sb.toString();
    }

    private static void appendImprovementLine(
            final StringBuilder sb,
            final String baselineName,
            final PolicyStats baseline,
            final PolicyStats ciavmp) {
        if (baseline == null) {
            return;
        }

        final double energyImprovement = relativeImprovementPercent(baseline.energyMean, ciavmp.energyMean);
        final double carbonImprovement = relativeImprovementPercent(baseline.carbonMean, ciavmp.carbonMean);
        final double makespanImprovement = relativeImprovementPercent(baseline.makespanMean, ciavmp.makespanMean);

        sb.append(String.format(
                Locale.US,
                "vs %s -> energy: %.2f%%, carbon: %.2f%%, makespan: %.2f%%%n",
                baselineName,
                energyImprovement,
                carbonImprovement,
                makespanImprovement
        ));
    }

    private static double relativeImprovementPercent(final double baseline, final double candidate) {
        if (baseline == 0.0) {
            return 0.0;
        }
        return ((baseline - candidate) / baseline) * 100.0;
    }

    private static HostEnergyAndCarbon estimateEnergyAndCarbon(
            final List<Cloudlet> cloudlets,
            final List<Host> hosts,
            final double makespanSeconds,
            final Map<Host, String> hostRegionMap,
            final CarbonIntensityProvider carbonIntensityProvider) {
        if (hosts.isEmpty() || makespanSeconds <= 0.0) {
            return new HostEnergyAndCarbon(0.0, 0.0);
        }

        double totalEnergyKwh = 0.0;
        double totalCarbonKg = 0.0;
        for (Host host : hosts) {
            final double hostCpuTime = cloudlets.stream()
                    .filter(Cloudlet::isFinished)
                    .filter(cloudlet -> cloudlet.getVm() != Vm.NULL)
                    .filter(cloudlet -> cloudlet.getVm().getHost() == host)
                    .mapToDouble(Cloudlet::getActualCpuTime)
                    .sum();

            if (hostCpuTime <= 0.0) {
                continue;
            }

            final int hostPes = Math.max(1, host.getWorkingPesNumber());
            final double utilization = Math.min(1.0, hostCpuTime / (makespanSeconds * hostPes));
            final double avgPowerWatts = IDLE_POWER_WATTS
                    + (MAX_POWER_WATTS - IDLE_POWER_WATTS) * utilization;
            final double hostEnergyKwh = (avgPowerWatts * makespanSeconds) / 3_600_000.0;
            final double carbonIntensity = carbonIntensityProvider.getIntensityGco2PerKwh(
                    hostRegionMap.getOrDefault(host, DEFAULT_REGION)
            );

            totalEnergyKwh += hostEnergyKwh;
            totalCarbonKg += (hostEnergyKwh * carbonIntensity) / 1000.0;
        }

        return new HostEnergyAndCarbon(totalEnergyKwh, totalCarbonKg);
    }

    public static final class EvaluationMetrics {
        private final String timestamp;
        private final int runId;
        private final String policyName;
        private final int totalCloudlets;
        private final long finishedCloudlets;
        private final double completionRate;
        private final double avgCpuTime;
        private final double makespan;
        private final double energyKwh;
        private final double carbonKg;

        public EvaluationMetrics(
                final String timestamp,
                final int runId,
                final String policyName,
                final int totalCloudlets,
                final long finishedCloudlets,
                final double completionRate,
                final double avgCpuTime,
                final double makespan,
                final double energyKwh,
                final double carbonKg) {
            this.timestamp = timestamp;
            this.runId = runId;
            this.policyName = policyName;
            this.totalCloudlets = totalCloudlets;
            this.finishedCloudlets = finishedCloudlets;
            this.completionRate = completionRate;
            this.avgCpuTime = avgCpuTime;
            this.makespan = makespan;
            this.energyKwh = energyKwh;
            this.carbonKg = carbonKg;
        }

        public String policyName() {
            return policyName;
        }

        public String toCsvLine() {
            return String.format(
                    Locale.US,
                    "%s,%d,%s,%d,%d,%.2f,%.2f,%.2f,%.6f,%.6f",
                    timestamp,
                    runId,
                    policyName,
                    totalCloudlets,
                    finishedCloudlets,
                    completionRate,
                    avgCpuTime,
                    makespan,
                    energyKwh,
                    carbonKg
            );
        }
    }

    private static final class PolicyStats {
        private final int runs;
        private final double completionMean;
        private final double completionStd;
        private final double makespanMean;
        private final double makespanStd;
        private final double energyMean;
        private final double energyStd;
        private final double carbonMean;
        private final double carbonStd;

        private PolicyStats(
                final int runs,
                final double completionMean,
                final double completionStd,
                final double makespanMean,
                final double makespanStd,
                final double energyMean,
                final double energyStd,
                final double carbonMean,
                final double carbonStd) {
            this.runs = runs;
            this.completionMean = completionMean;
            this.completionStd = completionStd;
            this.makespanMean = makespanMean;
            this.makespanStd = makespanStd;
            this.energyMean = energyMean;
            this.energyStd = energyStd;
            this.carbonMean = carbonMean;
            this.carbonStd = carbonStd;
        }

        private static PolicyStats from(final List<EvaluationMetrics> metrics) {
            final double completionMean = mean(metrics.stream().mapToDouble(m -> m.completionRate).toArray());
            final double makespanMean = mean(metrics.stream().mapToDouble(m -> m.makespan).toArray());
            final double energyMean = mean(metrics.stream().mapToDouble(m -> m.energyKwh).toArray());
            final double carbonMean = mean(metrics.stream().mapToDouble(m -> m.carbonKg).toArray());

            final double completionStd = stddev(metrics.stream().mapToDouble(m -> m.completionRate).toArray());
            final double makespanStd = stddev(metrics.stream().mapToDouble(m -> m.makespan).toArray());
            final double energyStd = stddev(metrics.stream().mapToDouble(m -> m.energyKwh).toArray());
            final double carbonStd = stddev(metrics.stream().mapToDouble(m -> m.carbonKg).toArray());

            return new PolicyStats(
                    metrics.size(),
                    completionMean,
                    completionStd,
                    makespanMean,
                    makespanStd,
                    energyMean,
                    energyStd,
                    carbonMean,
                    carbonStd
            );
        }

        private String toCsvLine(final String policyName) {
            return String.format(
                    Locale.US,
                    "%s,%d,%.4f,%.4f,%.4f,%.4f,%.6f,%.6f,%.6f,%.6f",
                    policyName,
                    runs,
                    completionMean,
                    completionStd,
                    makespanMean,
                    makespanStd,
                    energyMean,
                    energyStd,
                    carbonMean,
                    carbonStd
            );
        }

        private static double mean(final double[] values) {
            if (values.length == 0) {
                return 0.0;
            }

            double sum = 0.0;
            for (double value : values) {
                sum += value;
            }
            return sum / values.length;
        }

        private static double stddev(final double[] values) {
            if (values.length <= 1) {
                return 0.0;
            }

            final double mean = mean(values);
            double sumSquared = 0.0;
            for (double value : values) {
                final double delta = value - mean;
                sumSquared += delta * delta;
            }

            return Math.sqrt(sumSquared / (values.length - 1));
        }
    }

    private static final class HostEnergyAndCarbon {
        private final double energyKwh;
        private final double carbonKg;

        private HostEnergyAndCarbon(final double energyKwh, final double carbonKg) {
            this.energyKwh = energyKwh;
            this.carbonKg = carbonKg;
        }
    }
}
