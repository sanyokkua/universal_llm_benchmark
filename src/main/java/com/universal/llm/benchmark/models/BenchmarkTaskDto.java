package com.universal.llm.benchmark.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkTaskDto {
    private String taskId;
    private String category;
    private String subCategory;
    private String question;
    private BenchmarkTaskAnswerDto expectedAnswer;
    private String incorrectAnswerDirection;
}
