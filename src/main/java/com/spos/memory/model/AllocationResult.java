package com.spos.memory.model;

public final class AllocationResult {
    private final boolean success;
    private final String message;
    private final String processId;
    private final int requestedSize;
    private final Integer startAddress;
    private final AllocationAlgorithm algorithm;

    private AllocationResult(
            boolean success,
            String message,
            String processId,
            int requestedSize,
            Integer startAddress,
            AllocationAlgorithm algorithm) {
        this.success = success;
        this.message = message;
        this.processId = processId;
        this.requestedSize = requestedSize;
        this.startAddress = startAddress;
        this.algorithm = algorithm;
    }

    public static AllocationResult success(
            String processId,
            int requestedSize,
            int startAddress,
            AllocationAlgorithm algorithm) {
        return new AllocationResult(
                true,
                "Allocated " + requestedSize + " units to " + processId + " at address " + startAddress + ".",
                processId,
                requestedSize,
                startAddress,
                algorithm);
    }

    public static AllocationResult failure(
            String processId,
            int requestedSize,
            AllocationAlgorithm algorithm,
            String message) {
        return new AllocationResult(false, message, processId, requestedSize, null, algorithm);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getProcessId() {
        return processId;
    }

    public int getRequestedSize() {
        return requestedSize;
    }

    public Integer getStartAddress() {
        return startAddress;
    }

    public AllocationAlgorithm getAlgorithm() {
        return algorithm;
    }
}
