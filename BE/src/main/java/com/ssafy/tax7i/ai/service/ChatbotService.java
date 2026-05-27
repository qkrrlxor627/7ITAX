package com.ssafy.tax7i.ai.service;

import com.ssafy.tax7i.ai.client.AiServiceClient;
import com.ssafy.tax7i.ai.client.dto.AiChatHistoryResponse;
import com.ssafy.tax7i.ai.client.dto.AiChatRequest;
import com.ssafy.tax7i.ai.client.dto.AiChatResponse;
import com.ssafy.tax7i.ai.dto.ChatHistoryResponse;
import com.ssafy.tax7i.ai.dto.ChatMessage;
import com.ssafy.tax7i.ai.dto.ChatbotRequest;
import com.ssafy.tax7i.ai.dto.ChatbotResponse;
import com.ssafy.tax7i.ai.entity.ChatMessageEntity;
import com.ssafy.tax7i.ai.entity.ChatSession;
import com.ssafy.tax7i.ai.repository.ChatMessageRepository;
import com.ssafy.tax7i.ai.repository.ChatSessionRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final AiServiceClient aiClient;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiRateLimiter rateLimiter;

    public ChatbotResponse sendMessage(Long userId, ChatbotRequest request) {
        rateLimiter.checkChatLimit(userId);
        try {
            AiChatRequest aiRequest = new AiChatRequest(
                    request.message(),
                    request.sessionId(),
                    String.valueOf(userId)
            );
            AiChatResponse aiResponse = aiClient.chat(aiRequest);

            // DB에 대화 이력 저장
            persistConversation(userId, aiResponse.sessionId(),
                    request.message(), aiResponse.answer());

            return new ChatbotResponse(
                    aiResponse.answer(),
                    aiResponse.model(),
                    aiResponse.sessionId()
            );
        } catch (BusinessException e) {
            log.error("AI 챗봇 메시지 전송 실패: userId={}, sessionId={}, error={}",
                    userId, request.sessionId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("AI 챗봇 메시지 전송 중 예상치 못한 오류: userId={}, sessionId={}",
                    userId, request.sessionId(), e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE,
                    "AI 챗봇 서비스에 일시적인 문제가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getHistory(Long userId, String sessionId) {
        return getHistory(userId, sessionId, 0, 50);
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getHistory(Long userId, String sessionId, int page, int size) {
        // 세션 소유권 검증: DB에 세션이 있으면 소유자만 접근 가능.
        // 세션 미존재(DB 저장 전 또는 저장 실패)는 허용 — AI 서비스가 없는 세션은 빈 응답 반환.
        // session_id는 UUID이므로 추측을 통한 비인가 접근 리스크는 무시 가능.
        chatSessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            if (!session.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.CHAT_SESSION_ACCESS_DENIED);
            }
        });

        // 페이지네이션: BE DB에서 저장된 이력을 페이징 조회
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var messagePage = chatMessageRepository
                .findByChatSessionSessionIdOrderByCreatedAtDesc(sessionId, pageable);

        // DB에 이력이 있으면 DB 기반 응답 (페이지네이션 적용)
        if (messagePage.hasContent()) {
            // 최신순 조회 후 시간순으로 뒤집어서 반환
            var contents = new java.util.ArrayList<>(messagePage.getContent());
            java.util.Collections.reverse(contents);
            List<ChatMessage> messages = contents.stream()
                    .map(m -> new ChatMessage(m.getRole(), m.getContent()))
                    .toList();
            return new ChatHistoryResponse(
                    sessionId,
                    messages,
                    (int) messagePage.getTotalElements()
            );
        }

        // DB에 이력 없으면 AI 서비스에서 조회 (폴백)
        try {
            AiChatHistoryResponse aiResponse = aiClient.getChatHistory(sessionId);
            List<ChatMessage> messages = aiResponse.messages().stream()
                    .map(m -> new ChatMessage(m.role(), m.content()))
                    .toList();
            return new ChatHistoryResponse(
                    aiResponse.sessionId(),
                    messages,
                    aiResponse.messageCount()
            );
        } catch (BusinessException e) {
            log.error("AI 챗봇 이력 조회 실패: sessionId={}, error={}", sessionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("AI 챗봇 이력 조회 중 예상치 못한 오류: sessionId={}", sessionId, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE,
                    "AI 챗봇 이력 조회에 일시적인 문제가 발생했습니다.");
        }
    }

    private void persistConversation(Long userId, String sessionId,
                                     String userMessage, String assistantAnswer) {
        try {
            ChatSession session = getOrCreateSession(userId, sessionId);

            chatMessageRepository.save(ChatMessageEntity.builder()
                    .chatSession(session)
                    .role("user")
                    .content(userMessage)
                    .build());

            chatMessageRepository.save(ChatMessageEntity.builder()
                    .chatSession(session)
                    .role("assistant")
                    .content(assistantAnswer)
                    .build());

            session.incrementMessageCount(2);
            chatSessionRepository.save(session);

            log.debug("챗봇 대화 저장: sessionId={}, userId={}, messageCount={}",
                    sessionId, userId, session.getMessageCount());
        } catch (Exception e) {
            log.warn("챗봇 대화 DB 저장 실패 (응답은 정상): sessionId={}, error={}",
                    sessionId, e.getMessage());
        }
    }

    /**
     * 세션을 조회하거나 새로 생성한다.
     * UNIQUE 제약 위반(동시 첫 메시지) 시 재조회하여 메시지 유실을 방지한다.
     */
    private ChatSession getOrCreateSession(Long userId, String sessionId) {
        return chatSessionRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    try {
                        return chatSessionRepository.save(
                                ChatSession.builder()
                                        .sessionId(sessionId)
                                        .userId(userId)
                                        .build()
                        );
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // 동시 요청으로 UNIQUE 위반 → 이미 생성된 세션 재조회
                        log.info("세션 동시 생성 감지, 기존 세션 사용: sessionId={}", sessionId);
                        return chatSessionRepository.findBySessionId(sessionId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "세션 UNIQUE 위반 후 재조회 실패: " + sessionId));
                    }
                });
    }
}
