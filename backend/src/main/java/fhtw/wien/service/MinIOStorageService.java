package fhtw.wien.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.minio.*;
import io.minio.errors.*;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
public class MinIOStorageService {
    
    private static final Logger log = LoggerFactory.getLogger(MinIOStorageService.class);
    
    private final MinioClient minioClient;
    private final String bucketName;
    
    public MinIOStorageService(
            @Value("${minio.endpoint:http://minio:9000}") String endpoint,
            @Value("${minio.access-key:minioadmin}") String accessKey,
            @Value("${minio.secret-key:minioadmin}") String secretKey,
            @Value("${minio.bucket-name:documents}") String bucketName) {
        
        this.bucketName = bucketName;
        
        // Configure OkHttpClient with connection pooling and timeouts
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
        
        try {
            initializeBucket();
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to initialize MinIO storage", e);
        }
    }

    /**
     * Uploads a document to MinIO using streaming.
     * Accepts InputStream directly to avoid loading entire file into memory.
     *
     * @param documentId the document UUID
     * @param filename the original filename
     * @param contentType the MIME type
     * @param inputStream the file content as stream
     * @param size the file size in bytes
     * @return the generated MinIO object key
     */
    @CircuitBreaker(name = "minioService")
    @Retry(name = "minioService")
    public String uploadDocument(UUID documentId, String filename, String contentType, InputStream inputStream, long size) {
        String objectKey = generateObjectKey(documentId, filename);
        
        try {
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(inputStream, size, -1) // -1 means unknown part size (stream all)
                    .contentType(contentType)
                    .build();
            
            minioClient.putObject(putObjectArgs);
            return objectKey;
            
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to upload document to MinIO: documentId={}, filename={}", 
                    documentId, filename, e);
            throw new RuntimeException("Failed to upload document to storage", e);
        }
    }
    
    @CircuitBreaker(name = "minioService")
    @Retry(name = "minioService")
    public InputStream downloadDocumentStream(String objectKey) {
        try {
            log.debug("Downloading from MinIO: key={}", objectKey);
            
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build();
            
            return minioClient.getObject(getObjectArgs);
            
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to download document from MinIO: objectKey={}", objectKey, e);
            throw new RuntimeException("Failed to download document from storage", e);
        }
    }
    

    @CircuitBreaker(name = "minioService")
    @Retry(name = "minioService")
    public byte[] downloadDocument(String objectKey) {
        try (InputStream inputStream = downloadDocumentStream(objectKey)) {
            byte[] data = IOUtils.toByteArray(inputStream);
            log.debug("Downloaded to byte[]: key={}, size={} bytes", objectKey, data.length);
            return data;
        } catch (IOException e) {
            log.error("Failed to read document stream: objectKey={}", objectKey, e);
            throw new RuntimeException("Failed to read document from storage", e);
        }
    }
    

    @CircuitBreaker(name = "minioService")
    @Retry(name = "minioService")
    public void deleteDocument(String objectKey) {
        try {
            log.debug("Deleting from MinIO: key={}", objectKey);
            
            RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build();
            
            minioClient.removeObject(removeObjectArgs);
            
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to delete document from MinIO: objectKey={}", objectKey, e);
            throw new RuntimeException("Failed to delete document from storage", e);
        }
    }

    public String generateObjectKey(UUID documentId, String originalFilename) {
        // Create a hierarchical structure: documents/yyyy/mm/dd/documentId-filename
        String timestamp = java.time.LocalDate.now().toString().replace("-", "/");
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        return String.format("documents/%s/%s-%s", timestamp, documentId, sanitizedFilename);
    }
    

    public String getBucketName() {
        return bucketName;
    }

    private void initializeBucket() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            
            if (!bucketExists) {
                log.info("Creating MinIO bucket: {}", bucketName);
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Successfully created MinIO bucket: {}", bucketName);
            } else {
                log.info("MinIO bucket already exists: {}", bucketName);
            }
            
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to initialize MinIO bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to initialize MinIO bucket", e);
        }
    }
}