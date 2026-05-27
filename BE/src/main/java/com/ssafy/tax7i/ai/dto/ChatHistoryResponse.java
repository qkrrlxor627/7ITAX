package com.ssafy.tax7i.ai.dto;

import java.util.List;

public record ChatHistoryResponse(
        String sessionId,
        List<ChatMessage> messages,
        int messageCount
) {
}
