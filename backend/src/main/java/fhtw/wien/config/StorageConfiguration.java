package fhtw.wien.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * Configuration properties for document storage.
 * Centralizes storage configuration and provides factory methods.
 */
@Configuration
@ConfigurationProperties(prefix = "app.storage")
@Data
public class StorageConfiguration {
    
    private String defaultBucket = "local-storage";
    private String basePath = "documents";
    private String baseUri = "file://";
    
    /**
     * Generates a unique object key for a file.
     * 
     * @param originalFilename the original filename
     * @return the generated object key
     */
    public String generateObjectKey(String originalFilename) {
        return basePath + "/" + System.currentTimeMillis() + "-" + originalFilename;
    }
    
    /**
     * Generates a storage URI for a file.
     * 
     * @param filename the filename
     * @return the generated storage URI
     */
    public String generateStorageUri(String filename) {
        return baseUri + filename;
    }
}