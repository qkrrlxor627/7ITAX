package com.ssafy.tax7i.ai.client;

import com.ssafy.tax7i.ai.client.dto.*;
import com.ssafy.tax7i.config.AiServiceProperties;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Component
public class AiServiceClient {

    private final RestTemplate aiRestTemplate;
    private final AiServiceProperties properties;

    public AiServiceClient(@Qualifier("aiRestTemplate") RestTemplate aiRestTemplate,
                           AiServiceProperties properties) {
        this.aiRestTemplate = aiRestTemplate;
        this.properties = properties;
    }

    public AiChatResponse chat(AiChatRequest request) {
        return post("/api/v1/chat/", request, AiChatResponse.class);
    }

    public AiChatHistoryResponse getChatHistory(String sessionId) {
        return get("/api/v1/chat/history/" + sessionId, AiChatHistoryResponse.class);
    }

    public AiClassifyResponse classifyTransaction(String description) {
        AiClassifyRequest request = new AiClassifyRequest(description);
        return post("/api/v1/transaction/classify", request, AiClassifyResponse.class);
    }

    /**
     * AI 세목 분류 — 장애 시 Optional.empty() 반환 (graceful degradation)
     */
    public Optional<AiClassifyResponse> classifyTransactionSafe(String description) {
        try {
            return Optional.of(classifyTransaction(description));
        } catch (Exception e) {
            log.warn("AI 세목 분류 호출 실패 (기존 폴백 사용): {}", e.getMessage());
            return Optional.empty();
        }
    }

    private <T> T post(String path, Object request, Class<T> responseType) {
        String url = properties.baseUrl() + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);

        try {
            T response = aiRestTemplate.postForObject(url, entity, responseType);
            if (response == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 서비스 응답이 비어있습니다.");
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("AI 서비스 API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private <T> T get(String path, Class<T> responseType) {
        String url = properties.baseUrl() + path;

        try {
            T response = aiRestTemplate.getForObject(url, responseType);
            if (response == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 서비스 응답이 비어있습니다.");
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("AI 서비스 API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }
}
