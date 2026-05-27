package com.ssafy.tax7i.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiClassifyResponse(
        String category,
        float confidence,
        String method,
        String reason,
        @JsonProperty("legal_basis") String legalBasis
) {
}
