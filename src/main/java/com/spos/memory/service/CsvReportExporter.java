package com.spos.memory.service;

import com.spos.memory.model.HistoryEntry;
import com.spos.memory.model.MemoryBlock;
import com.spos.memory.model.MemoryStatistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CsvReportExporter {
    private CsvReportExporter() {
    }

    public static void export(Path destination, MemoryManager manager) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            MemoryStatistics statistics = manager.getStatistics();
            writer.write("section,timestamp,operation,success,details,total_memory,allocated_memory,free_memory,utilization_percent,largest_free_block,external_fragmentation,block_count,start_address,size,process_id");
            writer.newLine();
            writer.write(String.join(",", "summary", "", "", "", "", value(manager.getTotalMemory()),
                    value(statistics.getAllocatedMemory()), value(statistics.getFreeMemory()),
                    value(statistics.getUtilizationPercent()), value(statistics.getLargestFreeBlock()),
                    value(statistics.getExternalFragmentation()), value(statistics.getFreeBlockCount()), "", "", ""));
            writer.newLine();
            for (HistoryEntry entry : manager.getHistory()) {
                MemoryStatistics snapshot = entry.getStatistics();
                writer.write(String.join(",", "history", csv(entry.getFormattedTime()), csv(entry.getOperation().name()),
                        value(entry.isSuccess()), csv(entry.getDetails()), value(snapshot.getTotalMemory()),
                        value(snapshot.getAllocatedMemory()), value(snapshot.getFreeMemory()),
                        value(snapshot.getUtilizationPercent()), value(snapshot.getLargestFreeBlock()),
                        value(snapshot.getExternalFragmentation()), value(snapshot.getFreeBlockCount()), "", "", ""));
                writer.newLine();
            }
            for (MemoryBlock block : manager.getBlocks()) {
                writer.write(String.join(",", "block", "", "", "", "", value(manager.getTotalMemory()),
                        "", "", "", "", "", "", value(block.getStartAddress()), value(block.getSize()),
                        csv(block.isFree() ? "FREE" : block.getProcessId())));
                writer.newLine();
            }
        }
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
