package com.ssafy.tax7i.ai.dto;

public record ChatbotResponse(
        String answer,
        String model,
        String sessionId
) {
}
