package fhtw.wien.genaiworker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking processed messages to ensure idempotency in GenAI worker.
 */
@Service
public class IdempotencyService {
    
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private final Map<String, Instant> processedMessages = new ConcurrentHashMap<>();
    private static final long TTL_HOURS = 24;
    
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
    
    private void cleanupExpiredEntries() {
        Instant expirationTime = Instant.now().minus(TTL_HOURS, ChronoUnit.HOURS);
        processedMessages.entrySet().removeIf(entry -> entry.getValue().isBefore(expirationTime));
    }
    
    public int getCacheSize() {
        return processedMessages.size();
    }
}
