package io.callicode.rag.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DocumentIngestRequest(
        @NotEmpty(message = "texts must not be empty") List<String> texts
) {}
