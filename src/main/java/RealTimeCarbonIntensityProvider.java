import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RealTimeCarbonIntensityProvider implements CarbonIntensityProvider {

    private static final Pattern CARBON_INTENSITY_PATTERN =
            Pattern.compile("\"carbonIntensity\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

    private final HttpClient httpClient;
    private final String apiKey;
    private final String endpointTemplate;
    private final long cacheTtlMillis;
    private final double fallbackIntensity;
    private final Map<String, Double> fallbackByRegion;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public RealTimeCarbonIntensityProvider() {
        this(
                System.getenv("CARBON_API_KEY"),
                System.getenv().getOrDefault(
                        "CARBON_API_URL_TEMPLATE",
                        "https://api.electricitymap.org/v3/carbon-intensity/latest?zone=%s"),
                60_000L,
                500.0
        );
    }

    public RealTimeCarbonIntensityProvider(
            final String apiKey,
            final String endpointTemplate,
            final long cacheTtlMillis,
            final double fallbackIntensity) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.apiKey = apiKey;
        this.endpointTemplate = endpointTemplate;
        this.cacheTtlMillis = cacheTtlMillis;
        this.fallbackIntensity = fallbackIntensity;
        this.fallbackByRegion = Map.of(
                "US-CAL-CISO", 340.0,
                "US-MIDA-PJM", 430.0,
                "US-TEX-ERCO", 510.0,
                "US-NY-NYIS", 210.0
        );
    }

    @Override
    public double getIntensityGco2PerKwh(final String region) {
        final String normalizedRegion = normalizeRegion(region);
        final long now = System.currentTimeMillis();
        final CacheEntry cached = cache.get(normalizedRegion);
        if (cached != null && cached.expiresAtMillis > now) {
            return cached.intensity;
        }

        final double fetched = fetchIntensity(normalizedRegion);
        cache.put(normalizedRegion, new CacheEntry(fetched, now + cacheTtlMillis));
        return fetched;
    }

    private double fetchIntensity(final String region) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackByRegion.getOrDefault(region, fallbackIntensity);
        }

        final String encodedRegion = URLEncoder.encode(region, StandardCharsets.UTF_8);
        final String endpoint = String.format(endpointTemplate, encodedRegion);

        final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("auth-token", apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            final HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                final Matcher matcher = CARBON_INTENSITY_PATTERN.matcher(response.body());
                if (matcher.find()) {
                    return Double.parseDouble(matcher.group(1));
                }
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return fallbackByRegion.getOrDefault(region, fallbackIntensity);
    }

    private String normalizeRegion(final String region) {
        if (region == null || region.isBlank()) {
            return "US-CAL-CISO";
        }

        return region.trim();
    }

    private static final class CacheEntry {
        private final double intensity;
        private final long expiresAtMillis;

        private CacheEntry(final double intensity, final long expiresAtMillis) {
            this.intensity = intensity;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
