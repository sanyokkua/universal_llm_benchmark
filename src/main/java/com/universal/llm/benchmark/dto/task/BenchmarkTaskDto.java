package com.universal.llm.benchmark.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkTaskDto {
    private Long id;
    private String taskId;
    private String category;
    private String subcategory;
    private String question;
    private String excellent;
    private String good;
    private String pass;
    private String incorrectAnswerDirection;
    private LocalDateTime createdAt;
}
