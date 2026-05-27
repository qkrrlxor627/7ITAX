package com.ssafy.tax7i.sms.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.sms.dto.OtpSendResponse;
import com.ssafy.tax7i.sms.dto.OtpVerifyResponse;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsOtpService {

    private static final String OTP_PREFIX = "sms-otp:";
    private static final String OTP_ATTEMPTS_PREFIX = "sms-otp-attempts:";
    private static final String OTP_SEND_PREFIX = "sms-otp-send:";
    private static final String OTP_VERIFIED_PREFIX = "sms-otp-verified:";

    private static final int OTP_TTL_MINUTES = 3;
    private static final int OTP_VERIFIED_TTL_MINUTES = 5;
    private static final int OTP_SEND_TTL_MINUTES = 60;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int MAX_OTP_SENDS_PER_HOUR = 5;

    private final RedisTemplate<String, String> redisTemplate;
    private final SmsSender smsSender;
    private final UserRepository userRepository;

    public OtpSendResponse sendOtp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String phoneNumber = user.getPhoneNumber();

        String sendKey = OTP_SEND_PREFIX + userId;
        String sendCount = redisTemplate.opsForValue().get(sendKey);
        if (sendCount != null) {
            try {
                if (Long.parseLong(sendCount) >= MAX_OTP_SENDS_PER_HOUR) {
                    throw new BusinessException(ErrorCode.OTP_SEND_RATE_LIMITED);
                }
            } catch (NumberFormatException e) {
                redisTemplate.delete(sendKey);
            }
        }

        String otpCode = smsSender.generateOtp();

        redisTemplate.opsForValue().set(
                OTP_PREFIX + userId,
                otpCode,
                OTP_TTL_MINUTES, TimeUnit.MINUTES
        );

        redisTemplate.delete(OTP_ATTEMPTS_PREFIX + userId);

        smsSender.send(phoneNumber, otpCode);

        Long currentCount = redisTemplate.opsForValue().increment(sendKey);
        if (currentCount != null && currentCount == 1L) {
            redisTemplate.expire(sendKey, OTP_SEND_TTL_MINUTES, TimeUnit.MINUTES);
        }

        log.info("OTP 발송 완료: userId={}, maskedPhone={}", userId, smsSender.maskPhone(phoneNumber));

        return new OtpSendResponse(smsSender.maskPhone(phoneNumber), OTP_TTL_MINUTES * 60);
    }

    public OtpVerifyResponse verifyOtp(Long userId, String otpCode) {
        String attemptsKey = OTP_ATTEMPTS_PREFIX + userId;
        String attemptCount = redisTemplate.opsForValue().get(attemptsKey);
        if (attemptCount != null) {
            try {
                if (Long.parseLong(attemptCount) >= MAX_OTP_ATTEMPTS) {
                    throw new BusinessException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
                }
            } catch (NumberFormatException e) {
                redisTemplate.delete(attemptsKey);
            }
        }

        String otpKey = OTP_PREFIX + userId;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if (storedOtp == null) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }

        if (!storedOtp.equals(otpCode)) {
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, OTP_TTL_MINUTES, TimeUnit.MINUTES);
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptsKey);

        String token = TSID.fast().toString();
        redisTemplate.opsForValue().set(
                OTP_VERIFIED_PREFIX + token,
                String.valueOf(userId),
                OTP_VERIFIED_TTL_MINUTES, TimeUnit.MINUTES
        );

        log.info("OTP 인증 성공: userId={}", userId);

        return new OtpVerifyResponse(token, OTP_VERIFIED_TTL_MINUTES * 60);
    }

    public void validateOtpToken(Long userId, String otpToken) {
        String verifiedKey = OTP_VERIFIED_PREFIX + otpToken;
        String storedUserId = redisTemplate.opsForValue().get(verifiedKey);

        if (storedUserId == null) {
            throw new BusinessException(ErrorCode.OTP_TOKEN_INVALID);
        }

        if (!storedUserId.equals(String.valueOf(userId))) {
            throw new BusinessException(ErrorCode.OTP_TOKEN_INVALID);
        }

        redisTemplate.delete(verifiedKey);

        log.debug("OTP 토큰 검증 성공: userId={}", userId);
    }
}
