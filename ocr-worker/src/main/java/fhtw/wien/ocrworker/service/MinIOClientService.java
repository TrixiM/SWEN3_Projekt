package fhtw.wien.ocrworker.service;

import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * MinIO client service for the OCR worker to download documents from storage.
 * Provides read-only access to MinIO for OCR processing.
 */
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
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        
        log.info("MinIO client initialized for OCR worker: endpoint={}, bucket={}", endpoint, bucketName);
        
        try {
            validateConnection();
        } catch (Exception e) {
            log.error("Failed to validate MinIO connection", e);
            throw new RuntimeException("Failed to initialize MinIO connection", e);
        }
    }
    
    /**
     * Downloads a document from MinIO storage.
     *
     * @param objectKey the object key for the document
     * @return the document data as byte array
     * @throws RuntimeException if download fails
     */
    public byte[] downloadDocument(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
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
                
                log.debug("Successfully downloaded document: key={}, size={} bytes", 
                        objectKey, data.length);
                
                return data;
            }
            
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                log.error("Document not found in MinIO: {}", objectKey);
                throw new RuntimeException("Document not found: " + objectKey, e);
            }
            log.error("MinIO error downloading document: {}", objectKey, e);
            throw new RuntimeException("Failed to download document from MinIO", e);
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to download document from MinIO: objectKey={}", objectKey, e);
            throw new RuntimeException("Failed to download document from storage", e);
        }
    }
    
    /**
     * Checks if a document exists in MinIO storage.
     *
     * @param objectKey the object key for the document
     * @return true if the document exists, false otherwise
     */
    public boolean documentExists(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return false;
        }
        
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
     * Gets the metadata for a document in MinIO storage.
     *
     * @param objectKey the object key for the document
     * @return StatObjectResponse containing document metadata
     * @throws RuntimeException if getting metadata fails
     */
    public StatObjectResponse getDocumentMetadata(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Object key cannot be null or empty");
        }
        
        try {
            StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build();
            
            StatObjectResponse response = minioClient.statObject(statObjectArgs);
            
            log.debug("Retrieved metadata for document: key={}, size={} bytes, contentType={}", 
                    objectKey, response.size(), response.contentType());
            
            return response;
            
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to get document metadata from MinIO: objectKey={}", objectKey, e);
            throw new RuntimeException("Failed to get document metadata", e);
        }
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
     * Validates the MinIO connection and bucket access.
     *
     * @throws Exception if validation fails
     */
    private void validateConnection() throws Exception {
        try {
            // Check if bucket exists and is accessible
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            
            if (!bucketExists) {
                log.warn("MinIO bucket does not exist: {}. This may cause OCR processing to fail.", bucketName);
            } else {
                log.info("Successfully connected to MinIO bucket: {}", bucketName);
            }
            
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to validate MinIO connection: bucket={}", bucketName, e);
            throw new Exception("MinIO connection validation failed", e);
        }
    }
    
    /**
     * Checks if the MinIO service is healthy and accessible.
     *
     * @return true if MinIO is accessible, false otherwise
     */
    public boolean isHealthy() {
        try {
            minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            return true;
        } catch (Exception e) {
            log.warn("MinIO health check failed", e);
            return false;
        }
    }
}