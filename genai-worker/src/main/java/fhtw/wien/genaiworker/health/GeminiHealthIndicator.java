package fhtw.wien.genaiworker.health;

import fhtw.wien.genaiworker.service.GeminiService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Gemini API connectivity.
 */
@Component
public class GeminiHealthIndicator implements HealthIndicator {

    private final GeminiService geminiService;

    public GeminiHealthIndicator(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @Override
    public Health health() {
        try {
            boolean isConfigured = geminiService.isConfigured();
            
            if (isConfigured) {
                return Health.up()
                        .withDetail("status", "Configured")
                        .withDetail("api", "Gemini Pro")
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "Gemini API not configured")
                        .withDetail("message", "API key or URL missing")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
