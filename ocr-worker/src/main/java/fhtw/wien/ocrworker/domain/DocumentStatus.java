package fhtw.wien.ocrworker.domain;

/**
 * @deprecated Use fhtw.wien.domain.DocumentStatus from the shared backend module instead.
 * This enum is kept for backward compatibility but should not be used in new code.
 */
@Deprecated(since = "1.0", forRemoval = true)
public enum DocumentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    NEW;
    
    /**
     * Converts this enum to the shared DocumentStatus from backend.
     * @return corresponding backend DocumentStatus
     */
    public fhtw.wien.domain.DocumentStatus toBackendStatus() {
        return switch (this) {
            case NEW -> fhtw.wien.domain.DocumentStatus.NEW;
            case PENDING -> fhtw.wien.domain.DocumentStatus.OCR_PENDING;
            case PROCESSING -> fhtw.wien.domain.DocumentStatus.OCR_IN_PROGRESS;
            case COMPLETED -> fhtw.wien.domain.DocumentStatus.OCR_COMPLETED;
            case FAILED -> fhtw.wien.domain.DocumentStatus.OCR_FAILED;
        };
    }
    
    /**
     * Creates this enum from the shared DocumentStatus from backend.
     * @param backendStatus the backend status
     * @return corresponding OCR worker status
     */
    public static DocumentStatus fromBackendStatus(fhtw.wien.domain.DocumentStatus backendStatus) {
        return switch (backendStatus) {
            case NEW -> NEW;
            case OCR_PENDING -> PENDING;
            case OCR_IN_PROGRESS -> PROCESSING;
            case OCR_COMPLETED -> COMPLETED;
            case OCR_FAILED -> FAILED;
            default -> NEW; // Default fallback
        };
    }
}
