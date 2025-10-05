package fhtw.wien.ocrworker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OcrWorkerApplication {

    private static final Logger log = LoggerFactory.getLogger(OcrWorkerApplication.class);

    public static void main(String[] args) {
        log.info("🚀 Starting OCR Worker Application...");
        SpringApplication.run(OcrWorkerApplication.class, args);
        log.info("✅ OCR Worker Application started successfully");
    }
}
