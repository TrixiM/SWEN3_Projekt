package fhtw.wien.documentaccessbatchservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "batch.files")
public class FileProperties { //not used
    private Path inputDir;
    private Path archiveDir;
    private String filePattern;
}