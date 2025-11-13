package fhtw.wien.ocrworker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking processed messages to ensure idempotency in OCR worker.
 * <p>
 * NOTE: This implementation uses in-memory storage and is suitable for single-instance deployments.
 * For distributed systems, consider using Redis or a database-backed solution.
 */
@Service
public class IdempotencyService {
    
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final Duration MESSAGE_TTL = Duration.ofHours(24);
    
    private final Map<String, MessageRecord> processedMessages = new ConcurrentHashMap<>();
    
    /**
     * Attempts to mark a message as processed.
     * Returns true if this is the first time the message is being processed.
     * 
     * @param messageId unique message identifier
     * @return true if message can be processed, false if it's a duplicate
     */
    public boolean tryMarkAsProcessed(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalArgumentException("Message ID cannot be null or empty");
        }
        
        Instant now = Instant.now();
        MessageRecord newRecord = new MessageRecord(now);
        MessageRecord previousRecord = processedMessages.putIfAbsent(messageId, newRecord);
        
        if (previousRecord != null) {
            // Check if the previous record is still valid
            if (previousRecord.isExpired(now, MESSAGE_TTL)) {
                // Replace expired record
                processedMessages.put(messageId, newRecord);
                log.info("🔄 Replacing expired message record: {}", messageId);
                return true;
            }
            
            log.warn("⚠️ Duplicate message detected and rejected: {}", messageId);
            return false;
        }
        
        log.debug("✅ Message marked as processed: {}", messageId);
        return true;
    }
    
    /**
     * Checks if a message has been processed.
     * 
     * @param messageId unique message identifier
     * @return true if message was already processed
     */
    public boolean wasProcessed(String messageId) {
        MessageRecord record = processedMessages.get(messageId);
        if (record == null) {
            return false;
        }
        return !record.isExpired(Instant.now(), MESSAGE_TTL);
    }
    
    /**
     * Scheduled cleanup of expired message records.
     * Runs every hour to prevent memory leaks.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupExpiredEntries() {
        Instant now = Instant.now();
        int sizeBefore = processedMessages.size();
        
        processedMessages.entrySet().removeIf(entry -> 
                entry.getValue().isExpired(now, MESSAGE_TTL));
        
        int sizeAfter = processedMessages.size();
        int removed = sizeBefore - sizeAfter;
        
        if (removed > 0) {
            log.info("🧹 Cleaned up {} expired idempotency records (remaining: {})", 
                    removed, sizeAfter);
        }
    }
    
    /**
     * Gets the current number of tracked messages.
     * 
     * @return cache size
     */
    public int getCacheSize() {
        return processedMessages.size();
    }
    
    /**
     * Clears all tracked messages. Use with caution - mainly for testing.
     */
    public void clear() {
        processedMessages.clear();
        log.warn("⚠️ Idempotency cache cleared");
    }
    
    /**
     * Record of a processed message with timestamp.
     */
    private record MessageRecord(Instant processedAt) {
        
        boolean isExpired(Instant now, Duration ttl) {
            return processedAt.plus(ttl).isBefore(now);
        }
    }
}
