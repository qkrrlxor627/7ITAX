package com.ssafy.tax7i.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatbotRequest(
        @NotBlank String message,
        String sessionId
) {
}
