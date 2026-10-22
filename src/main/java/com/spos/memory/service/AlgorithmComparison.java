package com.spos.memory.service;

import com.spos.memory.model.AllocationAlgorithm;
import com.spos.memory.model.AllocationResult;
import com.spos.memory.model.MemoryStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AlgorithmComparison {
    private final List<Row> rows;

    private AlgorithmComparison(List<Row> rows) {
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
    }

    public static AlgorithmComparison run(MemoryManager initialState, List<AllocationRequest> requests) {
        List<Row> rows = new ArrayList<>();
        for (AllocationAlgorithm algorithm : AllocationAlgorithm.values()) {
            MemoryManager simulation = initialState.copyFor(algorithm);
            int successes = 0;
            for (AllocationRequest request : requests) {
                AllocationResult result = simulation.allocate(request.processId(), request.size());
                if (result.isSuccess()) {
                    successes++;
                }
            }
            rows.add(new Row(algorithm, successes, requests.size() - successes, simulation.getStatistics()));
        }
        initialState.recordComparison("Compared all algorithms across " + requests.size() + " queued requests.");
        return new AlgorithmComparison(rows);
    }

    public List<Row> getRows() {
        return rows;
    }

    public record Row(AllocationAlgorithm algorithm, int successes, int failures, MemoryStatistics statistics) {
    }
}
