package com.spos.memory.model;

import java.util.Objects;

public final class MemoryBlock {
    private int startAddress;
    private int size;
    private String processId;

    public MemoryBlock(int startAddress, int size, String processId) {
        if (startAddress < 0) {
            throw new IllegalArgumentException("Start address cannot be negative.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Block size must be positive.");
        }
        this.startAddress = startAddress;
        this.size = size;
        this.processId = processId;
    }

    public static MemoryBlock free(int startAddress, int size) {
        return new MemoryBlock(startAddress, size, null);
    }

    public static MemoryBlock allocated(int startAddress, int size, String processId) {
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("Allocated blocks require a process ID.");
        }
        return new MemoryBlock(startAddress, size, processId);
    }

    public int getStartAddress() {
        return startAddress;
    }

    public int getEndAddress() {
        return startAddress + size;
    }

    public int getSize() {
        return size;
    }

    public String getProcessId() {
        return processId;
    }

    public boolean isFree() {
        return processId == null;
    }

    public boolean isAllocated() {
        return processId != null;
    }

    public void setStartAddress(int startAddress) {
        this.startAddress = startAddress;
    }

    public void setSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Block size must be positive.");
        }
        this.size = size;
    }

    public void allocateTo(String processId) {
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("Process ID is required.");
        }
        this.processId = processId;
    }

    public void free() {
        this.processId = null;
    }

    public MemoryBlock copy() {
        return new MemoryBlock(startAddress, size, processId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MemoryBlock that)) {
            return false;
        }
        return startAddress == that.startAddress
                && size == that.size
                && Objects.equals(processId, that.processId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startAddress, size, processId);
    }

    @Override
    public String toString() {
        String occupant = isFree() ? "FREE" : processId;
        return String.format("[%d-%d) size=%d %s", startAddress, getEndAddress(), size, occupant);
    }
}
