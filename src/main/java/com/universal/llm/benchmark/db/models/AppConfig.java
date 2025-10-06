package com.universal.llm.benchmark.db.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfig {
    private long id;
    private String name;
    private String baseUrl;
}
