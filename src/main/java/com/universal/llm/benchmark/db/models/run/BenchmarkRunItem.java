package com.universal.llm.benchmark.db.models.run;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkRunItem {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benchmarkRunId")
    private BenchmarkRun benchmarkRun;

    private Long benchmarkTaskId;
    private Long targetProviderConfigId;
    private String targetProviderModelName;

    @Enumerated(value = EnumType.STRING)
    private BenchmarkRunItemStatus benchmarkItemStatus;

    private String taskLLMResultResponse;
    private String judgeResultResponse;
    private Integer evaluationScore;
    private String evaluationReason;
    private Integer timeTakenMs;
    private Integer tokensGenerated;
    private String errorMsg;
    private Boolean hasErrors;
    private Boolean isRecoverable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
