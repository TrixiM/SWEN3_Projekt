package fhtw.wien.ocrworker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final Duration TTL = Duration.ofHours(24); //processed message entries expire after 24 hours

    private final Map<String, Instant> processedMessages = new ConcurrentHashMap<>(); //stores messageID (key) and timestamp (value) as pair

    public boolean tryMarkAsProcessed(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            log.warn("Cannot mark empty message id as processed");
            return false;
        }

        cleanupExpiredEntries();

        Instant previousValue = processedMessages.putIfAbsent(messageId, Instant.now()); //inserts only if key is absent
        if (previousValue != null) { //messageId found
            log.warn("Duplicate message ignored: {}", messageId);
            return false;
        }

        return true;
    }
    //Removes messages older than 24 hours
    private void cleanupExpiredEntries() {
        Instant expirationTime = Instant.now().minus(TTL);
        processedMessages.entrySet().removeIf(entry -> entry.getValue().isBefore(expirationTime));
    }
}
