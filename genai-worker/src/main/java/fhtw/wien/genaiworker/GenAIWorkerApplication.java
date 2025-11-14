package fhtw.wien.genaiworker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * GenAI Worker Application
 * 
 * This microservice listens for OCR completion events via RabbitMQ,
 * generates document summaries using Google Gemini API, and publishes
 * the results back to the message queue for the backend to consume.
 */
@SpringBootApplication
@EnableAsync
public class GenAIWorkerApplication {

    private static final Logger log = LoggerFactory.getLogger(GenAIWorkerApplication.class);

    public static void main(String[] args) {
        log.info("🚀 Starting GenAI Worker Application...");
        SpringApplication.run(GenAIWorkerApplication.class, args);
        log.info("✅ GenAI Worker Application started successfully");
    }
}
