package com.spos.memory.service;

public record AllocationRequest(String processId, int size) {
    public AllocationRequest {
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("Process ID is required.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Process size must be greater than zero.");
        }
        processId = processId.trim();
    }
}
