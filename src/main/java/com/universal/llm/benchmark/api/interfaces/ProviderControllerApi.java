package com.universal.llm.benchmark.api.interfaces;

import java.util.List;

import com.universal.llm.benchmark.api.types.ApiResponse;
import com.universal.llm.benchmark.dto.provider.ProviderConfigDto;

public interface ProviderControllerApi extends CrudControllerApi<ProviderConfigDto> {
    ApiResponse<List<ProviderConfigDto>> getProvidersSortedByName();
    ApiResponse<List<ProviderConfigDto>> getProvidersSortedByBaseUrl();
}
