package fhtw.wien.config;


public final class MessagingConstants {
    
    private MessagingConstants() {
        // Constants class
    }
    
    // Exchange
    public static final String DOCUMENT_EXCHANGE = "document.exchange";
    
    // Queues we actually use
    public static final String DOCUMENT_CREATED_QUEUE = "document.created.queue";
    public static final String OCR_COMPLETED_QUEUE = "ocr.completed.queue";
    public static final String SUMMARY_RESULT_QUEUE = "summary.result.queue";

    // Routing Keys
    public static final String DOCUMENT_CREATED_ROUTING_KEY = "document.created";
    public static final String OCR_COMPLETED_ROUTING_KEY = "ocr.completed";
    public static final String SUMMARY_RESULT_ROUTING_KEY = "summary.result";
}
