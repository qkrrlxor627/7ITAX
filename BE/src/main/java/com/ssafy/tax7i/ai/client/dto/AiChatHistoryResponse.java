package com.ssafy.tax7i.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiChatHistoryResponse(
        @JsonProperty("session_id") String sessionId,
        List<AiChatMessage> messages,
        @JsonProperty("message_count") int messageCount
) {
}
