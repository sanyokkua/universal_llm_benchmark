package com.universal.llm.benchmark.dto.run;

import com.universal.llm.benchmark.db.models.run.BenchmarkRunStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkRunDto {
    private Long id;
    private String runId;
    private BenchmarkRunStatus benchmarkRunStatus;
    private Long judgeProviderConfigId;
    private String judgeProviderModelName;
    private LocalDateTime createdAt;
    private List<BenchmarkRunItemDto> items;
}
