package fhtw.wien.config;


public final class MessagingConstants {
    
    private MessagingConstants() {
        // Constants class
    }
    
    // Exchange
    public static final String DOCUMENT_EXCHANGE = "document.exchange";
    

    public static final String SUMMARY_RESULT_QUEUE = "summary.result.queue";

    // Routing Keys
    public static final String DOCUMENT_CREATED_ROUTING_KEY = "document.created";
    public static final String SUMMARY_RESULT_ROUTING_KEY = "summary.result";
    public static final String DOCUMENT_DELETED_ROUTING_KEY = "document.deleted";

}
