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
public class ProviderHeaderDto {
    private Long id;
    private Long providerConfigId;
    private String headerKey;
    private String headerValue;
    private Boolean isSecret;
    private LocalDateTime createdAt;
}
