package com.spos.memory.model;

public final class MemoryStatistics {
    private final int totalMemory;
    private final int allocatedMemory;
    private final int freeMemory;
    private final int largestFreeBlock;
    private final int freeBlockCount;
    private final int allocatedBlockCount;
    private final int externalFragmentation;

    public MemoryStatistics(
            int totalMemory,
            int allocatedMemory,
            int freeMemory,
            int largestFreeBlock,
            int freeBlockCount,
            int allocatedBlockCount,
            int externalFragmentation) {
        this.totalMemory = totalMemory;
        this.allocatedMemory = allocatedMemory;
        this.freeMemory = freeMemory;
        this.largestFreeBlock = largestFreeBlock;
        this.freeBlockCount = freeBlockCount;
        this.allocatedBlockCount = allocatedBlockCount;
        this.externalFragmentation = externalFragmentation;
    }

    public int getTotalMemory() {
        return totalMemory;
    }

    public int getAllocatedMemory() {
        return allocatedMemory;
    }

    public int getFreeMemory() {
        return freeMemory;
    }

    public int getLargestFreeBlock() {
        return largestFreeBlock;
    }

    public int getFreeBlockCount() {
        return freeBlockCount;
    }

    public int getAllocatedBlockCount() {
        return allocatedBlockCount;
    }

    public int getExternalFragmentation() {
        return externalFragmentation;
    }

    public double getUtilizationPercent() {
        if (totalMemory == 0) {
            return 0.0;
        }
        return (allocatedMemory * 100.0) / totalMemory;
    }

    public double getExternalFragmentationPercent() {
        if (freeMemory == 0) {
            return 0.0;
        }
        return (externalFragmentation * 100.0) / freeMemory;
    }
}
