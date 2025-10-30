package fhtw.wien.health;

import fhtw.wien.service.MinIOStorageService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for MinIO storage connectivity.
 */
@Component
public class MinIOHealthIndicator implements HealthIndicator {

    private final MinIOStorageService minioStorageService;

    public MinIOHealthIndicator(MinIOStorageService minioStorageService) {
        this.minioStorageService = minioStorageService;
    }

    @Override
    public Health health() {
        try {
            // Try to check bucket existence as a health check
            String bucketName = minioStorageService.getBucketName();
            
            // If we can get the bucket name without exception, consider it healthy
            if (bucketName != null && !bucketName.isEmpty()) {
                return Health.up()
                        .withDetail("bucket", bucketName)
                        .withDetail("status", "Connected")
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "Bucket name not configured")
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
