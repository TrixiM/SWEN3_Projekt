package fhtw.wien.ocrworker.messaging;

import fhtw.wien.ocrworker.config.RabbitMQConfig;
import fhtw.wien.ocrworker.dto.DocumentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OcrMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(OcrMessageConsumer.class);

    private final RabbitTemplate rabbitTemplate;

    public OcrMessageConsumer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_CREATED_QUEUE)
    public void handleDocumentCreated(DocumentResponse document) {
        log.info("📄 OCR WORKER RECEIVED: Document created - ID: {}, Title: '{}'",
                document.id(), document.title());
        
        log.info("📋 Document Details:");
        log.info("   - Filename: {}", document.originalFilename());
        log.info("   - Content Type: {}", document.contentType());
        log.info("   - Size: {} bytes", document.sizeBytes());
        log.info("   - Status: {}", document.status());
        
        // This is an "empty" OCR worker - it just processes/logs the message
        // In a real implementation, this would:
        // 1. Download the PDF from storage
        // 2. Run OCR on it (e.g., using Tesseract)
        // 3. Extract text
        // 4. Update the document status
        // 5. Store the extracted text
        
        log.info("🔄 Simulating OCR processing...");
        
        try {
            // Simulate some processing time
            Thread.sleep(1000);
            
            log.info("✅ OCR processing completed successfully for document: {}", document.id());
            
            // Send acknowledgment message back
            String ackMessage = String.format(
                    "✅ OCR Worker: Document '%s' (ID: %s) processed successfully",
                    document.title(), document.id()
            );
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_EXCHANGE,
                    "document.created.ack",
                    ackMessage
            );
            
            log.info("📤 Sent acknowledgment to queue");
            
        } catch (InterruptedException e) {
            log.error("❌ OCR processing interrupted for document: {}", document.id(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("❌ Error processing document: {}", document.id(), e);
        }
    }
}
