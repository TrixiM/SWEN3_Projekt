package fhtw.wien.ocrworker.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import okhttp3.OkHttpClient;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Service
public class MinIOClientService {

    private static final Logger log = LoggerFactory.getLogger(MinIOClientService.class);

    private final MinioClient minioClient;
    private final String bucketName;

    public MinIOClientService(
            @Value("${minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${minio.access-key:minioadmin}") String accessKey,
            @Value("${minio.secret-key:minioadmin}") String secretKey,
            @Value("${minio.bucket-name:documents}") String bucketName) {

        this.bucketName = bucketName;

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .httpClient(httpClient)
                .build();

        log.info("MinIO client initialized: endpoint={}, bucket={}", endpoint, bucketName);
    }

    @CircuitBreaker(name = "minioService")
    @Retry(name = "minioService")
    public byte[] downloadDocument(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Object key cannot be null or empty");
        }

        try {
            log.debug("Downloading document from MinIO: bucket={}, key={}", bucketName, objectKey);

            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build();

            try (InputStream inputStream = minioClient.getObject(getObjectArgs)) {
                byte[] data = IOUtils.toByteArray(inputStream);
                log.debug("Successfully downloaded document: key={}, size={} bytes", objectKey, data.length);
                return data;
            }

        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                log.error("Document not found in MinIO: {}", objectKey);
                throw new RuntimeException("Document not found: " + objectKey, e);
            }
            log.error("MinIO error downloading document: {}", objectKey, e);
            throw new RuntimeException("Failed to download document from MinIO", e);
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to download document from MinIO: objectKey={}", objectKey, e);
            throw new RuntimeException("Failed to download document from storage", e);
        } catch (Exception e) {
            log.error("Unexpected error downloading document from MinIO: objectKey={}", objectKey, e);
            throw new RuntimeException("Failed to download document from storage", e);
        }
    }
}
