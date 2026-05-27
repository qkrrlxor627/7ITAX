package com.ssafy.tax7i.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiChatResponse(
        String answer,
        String model,
        @JsonProperty("session_id") String sessionId
) {
}
