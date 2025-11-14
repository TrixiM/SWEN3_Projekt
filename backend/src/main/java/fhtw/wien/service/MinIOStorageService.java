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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling MinIO object storage operations for PDF documents.
 * Provides secure upload, download, and management of document files.
 */
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
     * Uploads a PDF document to MinIO storage.
     * Protected by circuit breaker and retry logic.
     *
     * @param documentId the unique document ID
     * @param filename the original filename
     * @param contentType the content type (should be application/pdf)
     * @param data the PDF file data
     * @return the object key for the uploaded document
     * @throws RuntimeException if upload fails
     */
    @CircuitBreaker(name = "minioService")
    @Retry(name = "minioService")
    public String uploadDocument(UUID documentId, String filename, String contentType, byte[] data) {
        String objectKey = generateObjectKey(documentId, filename);
        
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            log.info("Uploading document to MinIO: bucket={}, key={}, size={} bytes", 
                    bucketName, objectKey, data.length);
            
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(inputStream, data.length, -1)
                    .contentType(contentType)
                    .build();
            
            ObjectWriteResponse response = minioClient.putObject(putObjectArgs);
            
            log.info("Successfully uploaded document: etag={}, version={}", 
                    response.etag(), response.versionId());
            
            return objectKey;
            
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to upload document to MinIO: documentId={}, filename={}", 
                    documentId, filename, e);
            throw new RuntimeException("Failed to upload document to storage", e);
        }
    }
    
    /**
     * Downloads a PDF document from MinIO storage.
     * Protected by circuit breaker and retry logic.
     *
     * @param objectKey the object key for the document
     * @return the document data as byte array
     * @throws RuntimeException if download fails
     */
    @CircuitBreaker(name = "minioService")
    @Retry(name = "minioService")
    public byte[] downloadDocument(String objectKey) {
        try {
            log.info("Downloading document from MinIO: bucket={}, key={}", bucketName, objectKey);
            
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build();
            
            try (InputStream inputStream = minioClient.getObject(getObjectArgs)) {
                byte[] data = IOUtils.toByteArray(inputStream);
                
                log.info("Successfully downloaded document: key={}, size={} bytes", 
                        objectKey, data.length);
                
                return data;
            }
            
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to download document from MinIO: objectKey={}", objectKey, e);
            throw new RuntimeException("Failed to download document from storage", e);
        }
    }
    
    /**
     * Checks if a document exists in MinIO storage.
     * Protected by circuit breaker and retry logic.
     *
     * @param objectKey the object key for the document
     * @return true if the document exists, false otherwise
     */
    @CircuitBreaker(name = "minioService")
    @Retry(name = "minioService")
    public boolean documentExists(String objectKey) {
        try {
            StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build();
            
            minioClient.statObject(statObjectArgs);
            return true;
            
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                log.debug("Document not found: {}", objectKey);
                return false;
            }
            log.error("Error checking document existence: {}", objectKey, e);
            return false;
        } catch (Exception e) {
            log.error("Error checking document existence: {}", objectKey, e);
            return false;
        }
    }
    
    /**
     * Deletes a document from MinIO storage.
     * Protected by circuit breaker and retry logic.
     *
     * @param objectKey the object key for the document
     * @throws RuntimeException if deletion fails
     */
    @CircuitBreaker(name = "minioService")
    @Retry(name = "minioService")
    public void deleteDocument(String objectKey) {
        try {
            log.info("Deleting document from MinIO: bucket={}, key={}", bucketName, objectKey);
            
            RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build();
            
            minioClient.removeObject(removeObjectArgs);
            
            log.info("Successfully deleted document: key={}", objectKey);
            
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to delete document from MinIO: objectKey={}", objectKey, e);
            throw new RuntimeException("Failed to delete document from storage", e);
        }
    }
    
    /**
     * Gets the metadata for a document in MinIO storage.
     *
     * @param objectKey the object key for the document
     * @return StatObjectResponse containing document metadata
     * @throws RuntimeException if getting metadata fails
     */
    public StatObjectResponse getDocumentMetadata(String objectKey) {
        try {
            StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build();
            
            return minioClient.statObject(statObjectArgs);
            
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to get document metadata from MinIO: objectKey={}", objectKey, e);
            throw new RuntimeException("Failed to get document metadata", e);
        }
    }
    
    /**
     * Generates a unique object key for a document.
     *
     * @param documentId the document UUID
     * @param originalFilename the original filename
     * @return the generated object key
     */
    public String generateObjectKey(UUID documentId, String originalFilename) {
        // Create a hierarchical structure: documents/yyyy/mm/dd/documentId-filename
        String timestamp = java.time.LocalDate.now().toString().replace("-", "/");
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        return String.format("documents/%s/%s-%s", timestamp, documentId, sanitizedFilename);
    }
    
    /**
     * Gets the configured bucket name.
     *
     * @return the bucket name
     */
    public String getBucketName() {
        return bucketName;
    }
    
    /**
     * Initializes the MinIO bucket if it doesn't exist.
     */
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