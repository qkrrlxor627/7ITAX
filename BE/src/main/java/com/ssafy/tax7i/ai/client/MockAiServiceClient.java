package com.ssafy.tax7i.ai.client;

import com.ssafy.tax7i.ai.client.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true")
public class MockAiServiceClient extends AiServiceClient {

    public MockAiServiceClient() {
        super(null, null);
        log.info("[MOCK AI] MockAiServiceClient 활성화 - AI 서비스 호출을 Mock으로 대체합니다.");
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        log.info("[MOCK AI] chat 요청: {}", request.message());
        String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();
        return new AiChatResponse(
                "현재 AI 서비스가 Mock 모드입니다. 질문: " + request.message(),
                "mock",
                sessionId
        );
    }

    @Override
    public AiChatHistoryResponse getChatHistory(String sessionId) {
        log.info("[MOCK AI] 히스토리 조회: {}", sessionId);
        return new AiChatHistoryResponse(sessionId, List.of(), 0);
    }

    @Override
    public AiClassifyResponse classifyTransaction(String description) {
        log.info("[MOCK AI] 분류 요청: {}", description);
        return new AiClassifyResponse(
                "기타경비",
                0.5f,
                "mock",
                "Mock 모드 기본 분류",
                ""
        );
    }

    @Override
    public Optional<AiClassifyResponse> classifyTransactionSafe(String description) {
        return Optional.of(classifyTransaction(description));
    }
}
