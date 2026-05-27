package com.ssafy.tax7i.ai.service;

import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 기반 AI API 호출 Rate Limiter.
 * 사용자당 분당 최대 호출 횟수를 제한하여 AI 서비스 비용을 제어한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiRateLimiter {

    private static final int CHAT_MAX_PER_MINUTE = 10;
    private static final int CLASSIFY_MAX_PER_MINUTE = 30;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RedisTemplate<String, String> redisTemplate;

    public void checkChatLimit(Long userId) {
        checkLimit("ai:rate:chat:" + userId, CHAT_MAX_PER_MINUTE);
    }

    public void checkClassifyLimit(Long userId) {
        checkLimit("ai:rate:classify:" + userId, CLASSIFY_MAX_PER_MINUTE);
    }

    private void checkLimit(String key, int maxPerWindow) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, WINDOW);
            }
            if (count != null && count > maxPerWindow) {
                log.warn("AI Rate Limit 초과: key={}, count={}, max={}", key, count, maxPerWindow);
                throw new BusinessException(ErrorCode.AI_RATE_LIMITED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Redis 장애 시 rate limiting을 건너뛴다 (가용성 우선)
            log.warn("Rate Limiter Redis 오류 (제한 없이 통과): {}", e.getMessage());
        }
    }
}
