package com.universal.llm.benchmark.dto.provider;

import com.universal.llm.benchmark.db.models.provider.ProviderType;
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
public class ProviderConfigDto {
    private Long id;
    private String providerName;
    private ProviderType providerType;
    private String baseUrl;
    private String modelsEndpoint;
    private String inferenceEndpoint;
    private LocalDateTime createdAt;
    private List<ProviderHeaderDto> headers;
    private List<ProviderQueryParamDto> queryParams;
}
