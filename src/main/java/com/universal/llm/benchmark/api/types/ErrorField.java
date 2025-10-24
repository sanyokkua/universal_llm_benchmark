package com.universal.llm.benchmark.api.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorField {
    private Boolean hasError;
    private String errorName;
    private String errorMessage;
}
