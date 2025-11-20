package fhtw.wien.config;


public final class MessagingConstants {
    
    private MessagingConstants() {
        // Constants class
    }
    
    // Exchange
    public static final String DOCUMENT_EXCHANGE = "document.exchange";
    
    // Queues
    public static final String DOCUMENT_CREATED_QUEUE = "document.created.queue";
    public static final String DOCUMENT_DELETED_QUEUE = "document.deleted.queue";
    public static final String DOCUMENT_CREATED_ACK_QUEUE = "document.created.ack.queue";
    public static final String DOCUMENT_DELETED_ACK_QUEUE = "document.deleted.ack.queue";
    public static final String OCR_COMPLETED_QUEUE = "ocr.completed.queue";
    public static final String SUMMARY_RESULT_QUEUE = "summary.result.queue";
    
    // Routing Keys
    public static final String DOCUMENT_CREATED_ROUTING_KEY = "document.created";
    public static final String DOCUMENT_DELETED_ROUTING_KEY = "document.deleted";
    public static final String DOCUMENT_CREATED_ACK_ROUTING_KEY = "document.created.ack";
    public static final String DOCUMENT_DELETED_ACK_ROUTING_KEY = "document.deleted.ack";
    public static final String OCR_COMPLETED_ROUTING_KEY = "ocr.completed";
    public static final String SUMMARY_RESULT_ROUTING_KEY = "summary.result";
}