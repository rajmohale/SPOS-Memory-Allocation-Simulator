package com.spos.memory.service;

import com.spos.memory.model.AllocationAlgorithm;
import com.spos.memory.model.AllocationResult;
import com.spos.memory.model.MemoryBlock;
import com.spos.memory.service.CsvReportExporter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemoryManagerTest {
    @Test
    void selectsBlocksAccordingToEachFitAlgorithm() {
        MemoryManager firstFit = fragmentedMemory(AllocationAlgorithm.FIRST_FIT);
        MemoryManager bestFit = fragmentedMemory(AllocationAlgorithm.BEST_FIT);
        MemoryManager worstFit = fragmentedMemory(AllocationAlgorithm.WORST_FIT);

        assertEquals(0, firstFit.allocate("P", 25).getStartAddress());
        assertEquals(70, bestFit.allocate("P", 25).getStartAddress());
        assertEquals(0, worstFit.allocate("P", 25).getStartAddress());
    }

    @Test
    void deallocationMergesAdjacentFreeBlocks() {
        MemoryManager manager = new MemoryManager(100, AllocationAlgorithm.FIRST_FIT);
        manager.allocate("A", 25);
        manager.allocate("B", 25);
        manager.allocate("C", 25);
        manager.allocate("D", 25);

        assertTrue(manager.deallocate("B"));
        assertTrue(manager.deallocate("C"));

        List<MemoryBlock> blocks = manager.getBlocks();
        assertEquals(3, blocks.size());
        assertTrue(blocks.get(1).isFree());
        assertEquals(50, blocks.get(1).getSize());
        assertEquals(50, manager.getStatistics().getLargestFreeBlock());
    }

    @Test
    void rejectsInvalidAndDuplicateAllocationsWithoutChangingMemory() {
        MemoryManager manager = new MemoryManager(100, AllocationAlgorithm.FIRST_FIT);
        assertFalse(manager.allocate("", 10).isSuccess());
        assertFalse(manager.allocate("A", 0).isSuccess());
        assertTrue(manager.allocate("A", 20).isSuccess());

        AllocationResult duplicate = manager.allocate("A", 10);
        assertFalse(duplicate.isSuccess());
        assertEquals(20, manager.getStatistics().getAllocatedMemory());
        assertFalse(manager.allocate("B", 81).isSuccess());
        assertEquals(20, manager.getStatistics().getAllocatedMemory());
    }

    @Test
    void reportsFragmentationAndUtilization() {
        MemoryManager manager = fragmentedMemory(AllocationAlgorithm.FIRST_FIT);
        assertEquals(100, manager.getStatistics().getTotalMemory());
        assertEquals(20, manager.getStatistics().getAllocatedMemory());
        assertEquals(80, manager.getStatistics().getFreeMemory());
        assertEquals(50, manager.getStatistics().getLargestFreeBlock());
        assertEquals(30, manager.getStatistics().getExternalFragmentation());
        assertEquals(20.0, manager.getStatistics().getUtilizationPercent(), 0.0001);
    }

    @Test
    void comparisonRunsEveryStrategyAgainstIndependentCopies() {
        MemoryManager manager = fragmentedMemory(AllocationAlgorithm.FIRST_FIT);
        List<AllocationRequest> requests = List.of(new AllocationRequest("X", 25));

        AlgorithmComparison comparison = AlgorithmComparison.run(manager, requests);

        assertEquals(3, comparison.getRows().size());
        assertEquals(1, comparison.getRows().get(0).successes());
        assertEquals(1, comparison.getRows().get(1).successes());
        assertEquals(1, comparison.getRows().get(2).successes());
        assertEquals(20, manager.getStatistics().getAllocatedMemory());
        assertEquals("COMPARE", manager.getHistory().get(manager.getHistory().size() - 1).getOperation().name());
    }

    @Test
    void validatesInitialMemoryAndQueuedRequests() {
        assertThrows(IllegalArgumentException.class,
                () -> new MemoryManager(0, AllocationAlgorithm.FIRST_FIT));
        assertThrows(IllegalArgumentException.class, () -> new AllocationRequest("P", -1));
    }

    @Test
    void exportsSummaryHistoryAndMemoryBlocks() throws IOException {
        MemoryManager manager = new MemoryManager(100, AllocationAlgorithm.FIRST_FIT);
        manager.allocate("P,1", 25);
        Path report = Files.createTempFile("memory-simulator", ".csv");
        try {
            CsvReportExporter.export(report, manager);
            String contents = Files.readString(report);
            assertTrue(contents.contains("section,timestamp,operation"));
            assertTrue(contents.contains("summary,,,,,100,25,75,25.0,75,0,1"));
            assertTrue(contents.contains("\"P,1\""));
            assertTrue(contents.contains("history,"));
        } finally {
            Files.deleteIfExists(report);
        }
    }

    private static MemoryManager fragmentedMemory(AllocationAlgorithm algorithm) {
        MemoryManager manager = new MemoryManager(100, algorithm);
        manager.allocate("A", 50);
        manager.allocate("B", 20);
        manager.allocate("C", 30);
        manager.deallocate("A");
        manager.deallocate("C");
        return manager;
    }
}
