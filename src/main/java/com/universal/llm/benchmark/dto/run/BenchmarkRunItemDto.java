package com.universal.llm.benchmark.dto.run;

import com.universal.llm.benchmark.db.models.run.BenchmarkRunItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkRunItemDto {
    private Long id;
    private Long benchmarkRunId;
    private Long benchmarkTaskId;
    private Long targetProviderConfigId;
    private String targetProviderModelName;
    private BenchmarkRunItemStatus benchmarkItemStatus;
    private String taskLLMResultResponse;
    private String judgeResultResponse;
    private Integer evaluationScore;
    private String evaluationReason;
    private String errorMsg;
    private Integer timeTakenMs;
    private Integer tokensGenerated;

    private Integer attempts;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime nextRetryAt;
    private String workerId;
    private LocalDateTime inProgressAt;
    private String requestMeta;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
