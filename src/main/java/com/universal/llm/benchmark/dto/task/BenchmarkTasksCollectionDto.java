package com.universal.llm.benchmark.dto.task;

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
public class BenchmarkTasksCollectionDto {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private List<BenchmarkTaskDto> tasks;
}
