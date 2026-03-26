package io.callicode.rag.api.dto;

public record ChatResponse(String answer, String source, boolean cached) {}
