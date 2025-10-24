package com.universal.llm.benchmark.dto.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderQueryParamDto {
    private Long id;
    private Long providerConfigId;
    private String queryKey;
    private String queryValue;
    private LocalDateTime createdAt;
}
