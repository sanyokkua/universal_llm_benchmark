package com.universal.llm.benchmark.db.models.run;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkRun {
    @Id
    private Long id;
    private String runId;

    @Enumerated(EnumType.STRING)
    private BenchmarkRunStatus benchmarkRunStatus;

    private Long judgeProviderConfigId;
    private String judgeProviderModelName;
    private LocalDateTime createdAt;

    // One-to-Many relationship with BenchmarkRunItem
    @OneToMany(mappedBy = "benchmarkRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BenchmarkRunItem> runItems;
}
