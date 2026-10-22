package com.spos.memory.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class HistoryEntry {
    public enum Operation {
        INITIALIZE,
        ALLOCATE,
        DEALLOCATE,
        RESET,
        STEP,
        COMPARE
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LocalDateTime timestamp;
    private final Operation operation;
    private final boolean success;
    private final String details;
    private final MemoryStatistics statistics;

    public HistoryEntry(Operation operation, boolean success, String details, MemoryStatistics statistics) {
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.success = success;
        this.details = details;
        this.statistics = statistics;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        return timestamp.format(FORMATTER);
    }

    public Operation getOperation() {
        return operation;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getDetails() {
        return details;
    }

    public MemoryStatistics getStatistics() {
        return statistics;
    }
}
