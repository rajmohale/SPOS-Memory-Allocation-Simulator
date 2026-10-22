package com.spos.memory.service;

import com.spos.memory.model.AllocationAlgorithm;
import com.spos.memory.model.AllocationResult;
import com.spos.memory.model.HistoryEntry;
import com.spos.memory.model.MemoryBlock;
import com.spos.memory.model.MemoryStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MemoryManager {
    private final int totalMemory;
    private final List<MemoryBlock> blocks = new ArrayList<>();
    private final List<HistoryEntry> history = new ArrayList<>();
    private AllocationAlgorithm algorithm;

    public MemoryManager(int totalMemory, AllocationAlgorithm algorithm) {
        if (totalMemory <= 0) {
            throw new IllegalArgumentException("Total memory must be greater than zero.");
        }
        this.totalMemory = totalMemory;
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        blocks.add(MemoryBlock.free(0, totalMemory));
        record(HistoryEntry.Operation.INITIALIZE, true,
                "Initialized " + totalMemory + " units using " + algorithm + ".");
    }

    private MemoryManager(int totalMemory, AllocationAlgorithm algorithm, List<MemoryBlock> initialBlocks) {
        this.totalMemory = totalMemory;
        this.algorithm = algorithm;
        for (MemoryBlock block : initialBlocks) {
            blocks.add(block.copy());
        }
    }

    public synchronized AllocationResult allocate(String processId, int size) {
        String normalizedId = processId == null ? "" : processId.trim();
        if (normalizedId.isEmpty()) {
            return failedAllocation(normalizedId, size, "Process ID is required.");
        }
        if (size <= 0) {
            return failedAllocation(normalizedId, size, "Process size must be greater than zero.");
        }
        if (findProcess(normalizedId) != null) {
            return failedAllocation(normalizedId, size, "Process ID " + normalizedId + " is already allocated.");
        }

        int selected = selectBlock(size);
        if (selected < 0) {
            return failedAllocation(normalizedId, size,
                    "No free block can fit " + size + " units for " + normalizedId + ".");
        }

        MemoryBlock freeBlock = blocks.get(selected);
        int address = freeBlock.getStartAddress();
        if (freeBlock.getSize() == size) {
            blocks.set(selected, MemoryBlock.allocated(address, size, normalizedId));
        } else {
            int remaining = freeBlock.getSize() - size;
            blocks.set(selected, MemoryBlock.allocated(address, size, normalizedId));
            blocks.add(selected + 1, MemoryBlock.free(address + size, remaining));
        }
        AllocationResult result = AllocationResult.success(normalizedId, size, address, algorithm);
        record(HistoryEntry.Operation.ALLOCATE, true, result.getMessage());
        return result;
    }

    public synchronized boolean deallocate(String processId) {
        String normalizedId = processId == null ? "" : processId.trim();
        MemoryBlock allocated = findProcess(normalizedId);
        if (allocated == null) {
            record(HistoryEntry.Operation.DEALLOCATE, false,
                    "No allocated process found with ID " + (normalizedId.isEmpty() ? "(empty)" : normalizedId) + ".");
            return false;
        }
        allocated.free();
        mergeAdjacentFreeBlocks();
        record(HistoryEntry.Operation.DEALLOCATE, true, "Deallocated " + normalizedId + " and merged adjacent free blocks.");
        return true;
    }

    public synchronized void setAlgorithm(AllocationAlgorithm algorithm) {
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
    }

    public synchronized AllocationAlgorithm getAlgorithm() {
        return algorithm;
    }

    public int getTotalMemory() {
        return totalMemory;
    }

    public synchronized List<MemoryBlock> getBlocks() {
        List<MemoryBlock> snapshot = new ArrayList<>(blocks.size());
        for (MemoryBlock block : blocks) {
            snapshot.add(block.copy());
        }
        return Collections.unmodifiableList(snapshot);
    }

    public synchronized List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public synchronized MemoryStatistics getStatistics() {
        int allocated = 0;
        int free = 0;
        int largestFree = 0;
        int freeCount = 0;
        int allocatedCount = 0;
        for (MemoryBlock block : blocks) {
            if (block.isFree()) {
                free += block.getSize();
                largestFree = Math.max(largestFree, block.getSize());
                freeCount++;
            } else {
                allocated += block.getSize();
                allocatedCount++;
            }
        }
        return new MemoryStatistics(totalMemory, allocated, free, largestFree, freeCount,
                allocatedCount, Math.max(0, free - largestFree));
    }

    public synchronized MemoryManager copyFor(AllocationAlgorithm selectedAlgorithm) {
        return new MemoryManager(totalMemory, Objects.requireNonNull(selectedAlgorithm, "selectedAlgorithm"), blocks);
    }

    public synchronized void reset() {
        blocks.clear();
        blocks.add(MemoryBlock.free(0, totalMemory));
        history.clear();
        record(HistoryEntry.Operation.RESET, true, "Simulation reset to " + totalMemory + " free units.");
    }

    public synchronized void recordStep(String details, boolean success) {
        record(HistoryEntry.Operation.STEP, success, details);
    }

    public synchronized void recordComparison(String details) {
        record(HistoryEntry.Operation.COMPARE, true, details);
    }

    private AllocationResult failedAllocation(String processId, int size, String message) {
        AllocationResult result = AllocationResult.failure(processId, size, algorithm, message);
        record(HistoryEntry.Operation.ALLOCATE, false, message);
        return result;
    }

    private int selectBlock(int requestedSize) {
        int selected = -1;
        for (int index = 0; index < blocks.size(); index++) {
            MemoryBlock candidate = blocks.get(index);
            if (!candidate.isFree() || candidate.getSize() < requestedSize) {
                continue;
            }
            if (selected < 0) {
                selected = index;
                if (algorithm == AllocationAlgorithm.FIRST_FIT) {
                    break;
                }
                continue;
            }
            int selectedSize = blocks.get(selected).getSize();
            if (algorithm == AllocationAlgorithm.BEST_FIT && candidate.getSize() < selectedSize
                    || algorithm == AllocationAlgorithm.WORST_FIT && candidate.getSize() > selectedSize) {
                selected = index;
            }
        }
        return selected;
    }

    private MemoryBlock findProcess(String processId) {
        for (MemoryBlock block : blocks) {
            if (block.isAllocated() && block.getProcessId().equals(processId)) {
                return block;
            }
        }
        return null;
    }

    private void mergeAdjacentFreeBlocks() {
        for (int index = 0; index < blocks.size() - 1; ) {
            MemoryBlock current = blocks.get(index);
            MemoryBlock next = blocks.get(index + 1);
            if (current.isFree() && next.isFree()) {
                current.setSize(current.getSize() + next.getSize());
                blocks.remove(index + 1);
            } else {
                index++;
            }
        }
    }

    private void record(HistoryEntry.Operation operation, boolean success, String details) {
        history.add(new HistoryEntry(operation, success, details, getStatistics()));
    }
}
