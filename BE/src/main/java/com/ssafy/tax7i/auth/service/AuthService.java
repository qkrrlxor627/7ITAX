package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.domain.ConsentType;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.domain.UserStatus;
import com.ssafy.tax7i.auth.dto.ConsentRequest;
import com.ssafy.tax7i.auth.dto.IdentityVerifyRequest;
import com.ssafy.tax7i.auth.dto.IdentityVerifyResponse;
import com.ssafy.tax7i.auth.dto.LoginResponse;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.auth.service.NiceIdentityMockService.VerificationResult;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.global.jwt.JwtTokenProvider;
import com.ssafy.tax7i.sms.dto.OtpSendResponse;
import com.ssafy.tax7i.sms.service.SmsOtpService;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "BL:";
    private static final String VERIFY_SESSION_PREFIX = "verify-session:";
    private static final String PIN_FAIL_PREFIX = "pin-fail:";
    private static final String ADDITIONAL_AUTH_PREFIX = "login-additional-auth:";
    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final long PIN_FAIL_TTL_MINUTES = 5;
    private static final long ADDITIONAL_AUTH_TTL_MINUTES = 5;
    private static final int RECENT_FAIL_STEP_UP_THRESHOLD = 3;

    private final NiceIdentityMockService niceIdentityMockService;
    private final PinService pinService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final SsafyFinanceClient ssafyFinanceClient;
    private final ConsentService consentService;
    private final SmsOtpService smsOtpService;

    @Transactional
    public IdentityVerifyResponse verifyIdentity(IdentityVerifyRequest request) {
        VerificationResult result = niceIdentityMockService.verify(request);

        Optional<User> existing = userRepository.findByCi(result.ci());
        boolean isNewUser = existing.isEmpty();

        User user = existing.orElseGet(() -> {
            User newUser = userRepository.save(
                    User.builder()
                            .ci(result.ci())
                            .di(result.di())
                            .name(result.name())
                            .birthDate(LocalDate.parse(result.birthDate()))
                            .gender(result.gender())
                            .phoneNumber(result.phoneNumber())
                            .phoneLast4(result.phoneLast4())
                            .build()
            );
            assignSsafyUserKey(newUser);
            return newUser;
        });

        if (!isNewUser) {
            checkUserStatus(user);
            if (user.getSsafyUserKey() == null) {
                assignSsafyUserKey(user);
            }
        }

        boolean requiresPinSetup = user.getPinHash() == null;

        String verifyToken = TSID.fast().toString();
        redisTemplate.opsForValue().set(
                VERIFY_SESSION_PREFIX + verifyToken,
                String.valueOf(user.getId()),
                5, TimeUnit.MINUTES
        );

        return new IdentityVerifyResponse(user.getId(), isNewUser, requiresPinSetup, verifyToken);
    }

    @Transactional
    public LoginResponse setupPin(String verifyToken, String pin) {
        return setupPinInternal(verifyToken, pin, null, null, false);
    }

    @Transactional
    public LoginResponse setupPin(String verifyToken, String pin, String deviceId, List<ConsentRequest> consents) {
        return setupPinInternal(verifyToken, pin, deviceId, consents, true);
    }

    private LoginResponse setupPinInternal(
            String verifyToken,
            String pin,
            String deviceId,
            List<ConsentRequest> consents,
            boolean requireConsents
    ) {
        String userIdStr = redisTemplate.opsForValue().get(VERIFY_SESSION_PREFIX + verifyToken);
        if (userIdStr == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "본인인증 세션이 만료되었습니다.");
        }

        Long userId = Long.parseLong(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        checkUserStatus(user);
        if (requireConsents) {
            validateRequiredConsents(consents);
            consentService.saveConsents(userId, consents);
        }

        user.setupPin(pinService.hashPin(pin));
        if (StringUtils.hasText(deviceId)) {
            user.registerDevice(deviceId);
        }

        redisTemplate.delete(VERIFY_SESSION_PREFIX + verifyToken);

        return issueTokens(user.getId());
    }

    @Transactional
    public LoginResponse loginWithPin(String phoneNumber, String pin) {
        return loginWithPin(phoneNumber, pin, null, null, null, null);
    }

    @Transactional
    public LoginResponse loginWithPin(
            String phoneNumber,
            String pin,
            String deviceId,
            String deviceName,
            String ipAddress,
            String userAgent
    ) {
        String failKey = PIN_FAIL_PREFIX + phoneNumber;
        long recentFailCount = currentFailCount(failKey);
        if (recentFailCount >= MAX_PIN_ATTEMPTS) {
            throw new BusinessException(ErrorCode.PIN_ATTEMPTS_EXCEEDED);
        }

        String phoneLast4 = phoneNumber.substring(phoneNumber.length() - 4);
        List<User> candidates = userRepository.findByPhoneLast4(phoneLast4);

        User user = candidates.stream()
                .filter(u -> phoneNumber.equals(u.getPhoneNumber()))
                .findFirst()
                .orElse(null);

        if (user == null || user.getPinHash() == null || !pinService.verifyPin(pin, user.getPinHash())) {
            if (user != null) {
                redisTemplate.opsForValue().increment(failKey);
                redisTemplate.expire(failKey, PIN_FAIL_TTL_MINUTES, TimeUnit.MINUTES);
            }
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "휴대폰 번호 또는 PIN이 올바르지 않습니다.");
        }

        checkUserStatus(user);

        List<String> riskReasons = evaluateLoginRisk(user, deviceId, ipAddress, recentFailCount);
        if (!riskReasons.isEmpty()) {
            return createAdditionalAuthChallenge(user, deviceId, riskReasons);
        }

        redisTemplate.delete(failKey);
        registerTrustedDeviceIfNeeded(user, deviceId);
        user.updateLastLogin();
        return issueTokens(user.getId());
    }

    @Transactional
    public LoginResponse completeAdditionalAuth(String additionalAuthToken, String otpCode) {
        String challenge = redisTemplate.opsForValue().get(ADDITIONAL_AUTH_PREFIX + additionalAuthToken);
        if (!StringUtils.hasText(challenge)) {
            throw new BusinessException(ErrorCode.OTP_TOKEN_INVALID);
        }

        String[] parts = challenge.split("\\|", -1);
        Long userId = Long.parseLong(parts[0]);
        String deviceId = parts.length > 1 ? parts[1] : null;

        smsOtpService.verifyOtp(userId, otpCode);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        checkUserStatus(user);
        registerTrustedDeviceIfNeeded(user, deviceId);
        user.updateLastLogin();

        redisTemplate.delete(ADDITIONAL_AUTH_PREFIX + additionalAuthToken);
        redisTemplate.delete(PIN_FAIL_PREFIX + user.getPhoneNumber());

        return issueTokens(userId);
    }

    @Transactional(readOnly = true)
    public LoginResponse reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                newRefreshToken,
                jwtTokenProvider.getRefreshExpiration(),
                TimeUnit.MILLISECONDS
        );

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String accessToken) {
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserId(accessToken);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

        long remainingMs = jwtTokenProvider.getRemainingExpiration(accessToken);
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + accessToken,
                    "logout",
                    remainingMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    @Transactional
    public LoginResponse testLogin(String identifier) {
        String targetCi = (identifier != null && !identifier.isBlank())
                ? "test-ci-" + identifier
                : "test-ci-default";

        User user = userRepository.findByCi(targetCi)
                .orElseGet(() -> {
                    User newUser = userRepository.save(
                            User.builder()
                                    .ci(targetCi)
                                    .di("test-di-" + targetCi)
                                    .name("테스트 사용자")
                                    .birthDate(LocalDate.of(1990, 1, 1))
                                    .gender("M")
                                    .phoneNumber("01000000000")
                                    .phoneLast4("0000")
                                    .build()
                    );
                    assignSsafyUserKey(newUser);
                    return newUser;
                });

        return issueTokens(user.getId());
    }

    private LoginResponse issueTokens(Long userId) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                jwtTokenProvider.getRefreshExpiration(),
                TimeUnit.MILLISECONDS
        );

        return new LoginResponse(accessToken, refreshToken);
    }

    private void validateRequiredConsents(List<ConsentRequest> consents) {
        if (consents == null || consents.isEmpty()) {
            throw new BusinessException(ErrorCode.CONSENT_REQUIRED);
        }

        Set<ConsentType> agreedTypes = consents.stream()
                .filter(consent -> Boolean.TRUE.equals(consent.agreed()))
                .map(ConsentRequest::consentType)
                .collect(Collectors.toSet());

        if (!agreedTypes.containsAll(EnumSet.allOf(ConsentType.class))) {
            throw new BusinessException(ErrorCode.CONSENT_REQUIRED);
        }
    }

    private long currentFailCount(String failKey) {
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount == null) {
            return 0;
        }

        try {
            return Long.parseLong(failCount);
        } catch (NumberFormatException e) {
            redisTemplate.delete(failKey);
            return 0;
        }
    }

    private List<String> evaluateLoginRisk(User user, String deviceId, String ipAddress, long recentFailCount) {
        List<String> reasons = new ArrayList<>();
        if (StringUtils.hasText(deviceId)
                && StringUtils.hasText(user.getDeviceId())
                && !user.getDeviceId().equals(deviceId)) {
            reasons.add("NEW_DEVICE");
        }
        if (StringUtils.hasText(deviceId) && recentFailCount >= RECENT_FAIL_STEP_UP_THRESHOLD) {
            reasons.add("MULTIPLE_RECENT_PIN_FAILURES");
        }
        return reasons;
    }

    private LoginResponse createAdditionalAuthChallenge(User user, String deviceId, List<String> riskReasons) {
        String challengeToken = TSID.fast().toString();
        String challengeValue = user.getId() + "|" + (StringUtils.hasText(deviceId) ? deviceId : "");
        redisTemplate.opsForValue().set(
                ADDITIONAL_AUTH_PREFIX + challengeToken,
                challengeValue,
                ADDITIONAL_AUTH_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        OtpSendResponse otp = smsOtpService.sendOtp(user.getId());
        log.info("추가 인증 요구: userId={}, reasons={}", user.getId(), riskReasons);
        return LoginResponse.additionalAuthRequired(
                challengeToken,
                otp.maskedPhone(),
                otp.expiresInSeconds(),
                riskReasons
        );
    }

    private void registerTrustedDeviceIfNeeded(User user, String deviceId) {
        if (StringUtils.hasText(deviceId) && !deviceId.equals(user.getDeviceId())) {
            user.registerDevice(deviceId);
        }
    }

    private void assignSsafyUserKey(User user) {
        try {
            String memberId = "tax7i-user-" + user.getId() + "@tax7i.dev";
            String userKey = ssafyFinanceClient.getOrRegisterMember(memberId);
            user.assignSsafyUserKey(userKey);
            log.info("SSAFY 멤버 등록 성공: userId={}, userKey={}", user.getId(), userKey);
        } catch (Exception e) {
            log.error("SSAFY 멤버 등록 실패: userId={}", user.getId(), e);
        }
    }

    private void checkUserStatus(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.USER_WITHDRAWN);
        }
    }
}
