package com.spos.memory.model;

public enum AllocationAlgorithm {
    FIRST_FIT("First Fit"),
    BEST_FIT("Best Fit"),
    WORST_FIT("Worst Fit");

    private final String displayName;

    AllocationAlgorithm(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
