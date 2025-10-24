package com.universal.llm.benchmark.db.models.provider;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderConfig {
    @Id
    private Long id; // Auto-increment ID
    private String providerName; // User-Friendly (provided) name of the provider, like Ollama Local, can be null, fallback to baseUrl

    @Enumerated(EnumType.STRING)
    private ProviderType providerType;
    private String baseUrl;
    private String modelsEndpoint;
    private String inferenceEndpoint;
    private LocalDateTime createdAt;

    // One-to-Many relationship with ProviderHeader
    @OneToMany(mappedBy = "providerConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProviderHeader> headers;
    // One-to-Many relationship with ProviderQueryParam
    @OneToMany(mappedBy = "providerConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProviderQueryParam> queryParams;

    // ProviderConfig can have 0 or several ProviderHeader
    // ProviderConfig can have 0 or several ProviderQueryParam
}
