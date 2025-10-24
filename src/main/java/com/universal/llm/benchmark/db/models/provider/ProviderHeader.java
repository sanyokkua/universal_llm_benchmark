package com.universal.llm.benchmark.db.models.provider;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderHeader {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "providerConfigId")
    private ProviderConfig providerConfig;

    private String headerKey;
    private String headerValue;
    private Boolean isSecret;
    private LocalDateTime createdAt;
}
