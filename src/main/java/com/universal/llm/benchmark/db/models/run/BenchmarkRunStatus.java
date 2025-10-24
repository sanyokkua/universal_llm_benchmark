package com.universal.llm.benchmark.db.models.run;

import lombok.Getter;

@Getter
public enum BenchmarkRunStatus {
    PENDING("PENDING"),
    FINISHED("FINISHED");

    private final String value;

    BenchmarkRunStatus(String value) {
        this.value = value;
    }
}
