package fhtw.wien.ocrworker.domain;

public enum DocumentStatus {
    NEW,
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    public boolean isFinished() {
        return this == COMPLETED || this == FAILED;
    }
    public boolean isActive() {
        return this == PROCESSING || this == PENDING;
    }
}
