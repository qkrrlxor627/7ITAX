package com.ssafy.tax7i.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiChatRequest(
        String message,
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("user_id") String userId
) {
}
