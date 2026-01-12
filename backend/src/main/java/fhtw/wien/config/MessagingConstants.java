package fhtw.wien.config;


public final class MessagingConstants {
    
    private MessagingConstants() {
        // Constants class
    }
    
    // Exchange
    public static final String DOCUMENT_EXCHANGE = "document.exchange";

    //Queues
    public static final String SUMMARY_RESULT_QUEUE = "summary.result.queue";
    public static final String DOCUMENT_ACCESS_STATS_QUEUE = "document.access.stats.queue";

    // Routing Keys
    public static final String DOCUMENT_CREATED_ROUTING_KEY = "document.created";
    public static final String SUMMARY_RESULT_ROUTING_KEY = "summary.result";
    public static final String DOCUMENT_DELETED_ROUTING_KEY = "document.deleted";
    public static final String DOCUMENT_ACCESS_STATS_ROUTING_KEY = "document.access.stats";

}
