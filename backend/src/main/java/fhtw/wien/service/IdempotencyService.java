package fhtw.wien.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking processed messages to ensure idempotency.
 * Uses an in-memory cache with TTL to prevent duplicate message processing.
 * For production, consider using Redis or a database for distributed systems.
 */
@Service
public class IdempotencyService {
    
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    
    // Cache structure: messageId -> processing timestamp
    private final Map<String, Instant> processedMessages = new ConcurrentHashMap<>();
    
    // TTL for processed messages (24 hours)
    private static final long TTL_HOURS = 24;
    
    /**
     * Checks if a message has already been processed.
     * Also performs cleanup of expired entries.
     *
     * @param messageId the unique message identifier
     * @return true if message was already processed, false otherwise
     */
    public boolean isProcessed(String messageId) {
        cleanupExpiredEntries();
        
        boolean isProcessed = processedMessages.containsKey(messageId);
        
        if (isProcessed) {
            log.warn("⚠️ Duplicate message detected: {}", messageId);
        }
        
        return isProcessed;
    }
    
    /**
     * Marks a message as processed.
     *
     * @param messageId the unique message identifier
     */
    public void markAsProcessed(String messageId) {
        processedMessages.put(messageId, Instant.now());
        log.debug("✅ Message marked as processed: {}", messageId);
    }
    
    /**
     * Attempts to mark a message as processing if it hasn't been processed yet.
     * This is an atomic operation for idempotency.
     *
     * @param messageId the unique message identifier
     * @return true if message can be processed (not a duplicate), false if it's a duplicate
     */
    public boolean tryMarkAsProcessed(String messageId) {
        cleanupExpiredEntries();
        
        Instant previousValue = processedMessages.putIfAbsent(messageId, Instant.now());
        
        if (previousValue != null) {
            log.warn("⚠️ Duplicate message detected and rejected: {}", messageId);
            return false;
        }
        
        log.debug("✅ Message can be processed: {}", messageId);
        return true;
    }
    
    /**
     * Removes expired entries from the cache to prevent memory leaks.
     */
    private void cleanupExpiredEntries() {
        Instant expirationTime = Instant.now().minus(TTL_HOURS, ChronoUnit.HOURS);
        
        processedMessages.entrySet().removeIf(entry -> {
            boolean isExpired = entry.getValue().isBefore(expirationTime);
            if (isExpired) {
                log.trace("🗑️ Removing expired idempotency entry: {}", entry.getKey());
            }
            return isExpired;
        });
    }
    
    /**
     * Gets the current cache size (for monitoring/debugging).
     */
    public int getCacheSize() {
        return processedMessages.size();
    }
    
    /**
     * Clears all processed message entries (useful for testing).
     */
    public void clear() {
        processedMessages.clear();
        log.info("🧹 Idempotency cache cleared");
    }
}
