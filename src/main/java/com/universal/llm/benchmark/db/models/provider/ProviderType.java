package com.universal.llm.benchmark.db.models.provider;

import lombok.Getter;

@Getter
public enum ProviderType {
    OPENAI("OPENAI"),
    OLLAMA("OLLAMA"),
    LM_STUDIO("LM_STUDIO"),
    OPENROUTER("OPENROUTER"),
    CUSTOM_OPEN_AI_COMPATIBLE("CUSTOM_OPEN_AI_COMPATIBLE");

    private final String name;

    ProviderType(String name) {
        this.name = name;
    }
}
