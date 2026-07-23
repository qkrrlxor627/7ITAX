package com.ssafy.tax7i.payment.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.auth.service.PinService;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 결제(간편) 비밀번호 관리 서비스.
 * 로그인 PIN({@link User#getPinHash()})과 분리된 결제 전용 PIN을 BCrypt 해시로 저장·검증한다.
 */
@Service
@RequiredArgsConstructor
public class PayPinService {

    private static final String PAY_PIN_FAIL_PREFIX = "pay-pin-fail:";
    private static final int MAX_PAY_PIN_ATTEMPTS = 5;
    private static final long PAY_PIN_FAIL_TTL_MINUTES = 5;

    private final UserRepository userRepository;
    private final PinService pinService;
    private final RedisTemplate<String, String> redisTemplate;

    /** 결제 비밀번호 설정/변경. */
    @Transactional
    public void setPayPin(Long userId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setupPayPin(pinService.hashPin(pin));
    }

    /** 결제 비밀번호 설정 여부. */
    @Transactional(readOnly = true)
    public boolean hasPayPin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return user.hasPayPin();
    }

    /**
     * 결제 비밀번호 검증. 불일치/미설정 시 예외.
     * 연속 실패가 최대 시도횟수({@code MAX_PAY_PIN_ATTEMPTS})에 도달하면
     * {@code PAY_PIN_FAIL_TTL_MINUTES}분간 잠금(PIN_ATTEMPTS_EXCEEDED). 검증 성공 시 실패 카운터 초기화.
     */
    @Transactional(readOnly = true)
    public void verifyPayPin(Long userId, String pin) {
        String failKey = PAY_PIN_FAIL_PREFIX + userId;
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null) {
            try {
                if (Long.parseLong(failCount) >= MAX_PAY_PIN_ATTEMPTS) {
                    throw new BusinessException(ErrorCode.PIN_ATTEMPTS_EXCEEDED);
                }
            } catch (NumberFormatException e) {
                redisTemplate.delete(failKey);
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!user.hasPayPin()) {
            throw new BusinessException(ErrorCode.PIN_NOT_SET, "결제 비밀번호가 설정되지 않았습니다.");
        }
        if (!pinService.verifyPin(pin, user.getPayPinHash())) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, PAY_PIN_FAIL_TTL_MINUTES, TimeUnit.MINUTES);
            throw new BusinessException(ErrorCode.PIN_INVALID, "결제 비밀번호가 올바르지 않습니다.");
        }

        redisTemplate.delete(failKey);
    }
}
