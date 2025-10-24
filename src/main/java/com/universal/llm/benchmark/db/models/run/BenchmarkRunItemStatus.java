package com.universal.llm.benchmark.db.models.run;

import lombok.Getter;

@Getter
public enum BenchmarkRunItemStatus {
    PENDING("PENDING"),
    WAITING_FOR_JUDGE("WAITING_FOR_JUDGE"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String value;

    BenchmarkRunItemStatus(String value) {
        this.value = value;
    }
}
