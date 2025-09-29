package com.universal.llm.benchmark.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkTaskAnswerDto {
    private String excellent;
    private String good;
    private String pass;
}
