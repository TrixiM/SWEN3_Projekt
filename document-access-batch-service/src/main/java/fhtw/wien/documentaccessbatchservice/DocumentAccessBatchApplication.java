package fhtw.wien.documentaccessbatchservice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan("fhtw.wien.documentaccessbatchservice.config")
public class DocumentAccessBatchApplication {
    private static final Logger log = LoggerFactory.getLogger(DocumentAccessBatchApplication.class);

    public static void main(String[] args) {
        log.info("🚀 Starting Document Access Batch Service...");
        SpringApplication.run(DocumentAccessBatchApplication.class, args);
        log.info("✅ Document Access Batch Service started successfully");
    }
}
