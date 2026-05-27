package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.domain.UserStatus;
import com.ssafy.tax7i.auth.dto.IdentityVerifyRequest;
import com.ssafy.tax7i.auth.dto.IdentityVerifyResponse;
import com.ssafy.tax7i.auth.dto.LoginResponse;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.auth.service.NiceIdentityMockService.VerificationResult;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.global.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private NiceIdentityMockService niceIdentityMockService;
    @Mock private PinService pinService;
    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private SsafyFinanceClient ssafyFinanceClient;

    @InjectMocks
    private AuthService authService;

    // ───────────── verifyIdentity ─────────────

    @Test
    void verifyIdentity_신규유저_회원생성() {
        IdentityVerifyRequest request = new IdentityVerifyRequest("홍길동", "1990-01-01", "M", "01012345678");
        VerificationResult result = new VerificationResult(
                "ci-hash", "di-hash", "홍길동", "1990-01-01", "M", "01012345678", "5678");

        given(niceIdentityMockService.verify(request)).willReturn(result);
        given(userRepository.findByCi("ci-hash")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User u = invocation.getArgument(0);
            setField(u, "id", 1L);
            return u;
        });
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        IdentityVerifyResponse response = authService.verifyIdentity(request);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.requiresPinSetup()).isTrue();
        assertThat(response.verifyToken()).isNotNull();
        then(userRepository).should().save(any(User.class));
    }

    @Test
    void verifyIdentity_기존유저_PIN있음() {
        IdentityVerifyRequest request = new IdentityVerifyRequest("홍길동", "1990-01-01", "M", "01012345678");
        VerificationResult result = new VerificationResult(
                "ci-hash", "di-hash", "홍길동", "1990-01-01", "M", "01012345678", "5678");
        User existingUser = createUser(1L);
        setField(existingUser, "pinHash", "encoded-pin");

        given(niceIdentityMockService.verify(request)).willReturn(result);
        given(userRepository.findByCi("ci-hash")).willReturn(Optional.of(existingUser));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        IdentityVerifyResponse response = authService.verifyIdentity(request);

        assertThat(response.isNewUser()).isFalse();
        assertThat(response.requiresPinSetup()).isFalse();
        assertThat(response.verifyToken()).isNotNull();
        then(userRepository).should(never()).save(any(User.class));
    }

    // ───────────── setupPin ─────────────

    @Test
    void setupPin_성공_JWT발급() {
        User user = createUser(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("verify-session:valid-token")).willReturn("1");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(pinService.hashPin("123456")).willReturn("encoded-pin");
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshExpiration()).willReturn(604800000L);

        LoginResponse response = authService.setupPin("valid-token", "123456");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        then(redisTemplate).should().delete("verify-session:valid-token");
    }

    @Test
    void setupPin_세션만료_예외() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("verify-session:expired-token")).willReturn(null);

        assertThatThrownBy(() -> authService.setupPin("expired-token", "123456"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    // ───────────── loginWithPin ─────────────

    @Test
    void loginWithPin_성공() {
        User user = createUser(1L);
        setField(user, "pinHash", "encoded-pin");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("pin-fail:01012345678")).willReturn(null);
        given(userRepository.findByPhoneLast4("5678")).willReturn(List.of(user));
        given(pinService.verifyPin("123456", "encoded-pin")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshExpiration()).willReturn(604800000L);

        LoginResponse response = authService.loginWithPin("01012345678", "123456");

        assertThat(response.accessToken()).isEqualTo("access-token");
        then(redisTemplate).should().delete("pin-fail:01012345678");
    }

    @Test
    void loginWithPin_사용자없음_예외() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("pin-fail:01099999999")).willReturn(null);
        given(userRepository.findByPhoneLast4("9999")).willReturn(List.of());

        assertThatThrownBy(() -> authService.loginWithPin("01099999999", "123456"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void loginWithPin_PIN불일치_예외() {
        User user = createUser(1L);
        setField(user, "pinHash", "encoded-pin");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("pin-fail:01012345678")).willReturn(null);
        given(userRepository.findByPhoneLast4("5678")).willReturn(List.of(user));
        given(pinService.verifyPin("wrong", "encoded-pin")).willReturn(false);
        given(valueOperations.increment("pin-fail:01012345678")).willReturn(1L);

        assertThatThrownBy(() -> authService.loginWithPin("01012345678", "wrong"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED));

        then(valueOperations).should().increment("pin-fail:01012345678");
    }

    @Test
    void loginWithPin_PIN미설정_예외() {
        User user = createUser(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("pin-fail:01012345678")).willReturn(null);
        given(userRepository.findByPhoneLast4("5678")).willReturn(List.of(user));

        assertThatThrownBy(() -> authService.loginWithPin("01012345678", "123456"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void loginWithPin_정지된사용자_예외() {
        User user = createUser(1L);
        setField(user, "pinHash", "encoded-pin");
        user.suspend();
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("pin-fail:01012345678")).willReturn(null);
        given(userRepository.findByPhoneLast4("5678")).willReturn(List.of(user));
        given(pinService.verifyPin("123456", "encoded-pin")).willReturn(true);

        assertThatThrownBy(() -> authService.loginWithPin("01012345678", "123456"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_SUSPENDED));
    }

    // ───────────── PIN 브루트포스 방어 ─────────────

    @Test
    void loginWithPin_5회초과_차단() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("pin-fail:01012345678")).willReturn("5");

        assertThatThrownBy(() -> authService.loginWithPin("01012345678", "123456"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PIN_ATTEMPTS_EXCEEDED));

        then(userRepository).should(never()).findByPhoneLast4(any());
    }

    @Test
    void loginWithPin_성공시_카운터리셋() {
        User user = createUser(1L);
        setField(user, "pinHash", "encoded-pin");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("pin-fail:01012345678")).willReturn("3");
        given(userRepository.findByPhoneLast4("5678")).willReturn(List.of(user));
        given(pinService.verifyPin("123456", "encoded-pin")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshExpiration()).willReturn(604800000L);

        LoginResponse response = authService.loginWithPin("01012345678", "123456");

        assertThat(response.accessToken()).isEqualTo("access-token");
        then(redisTemplate).should().delete("pin-fail:01012345678");
    }

    @Test
    void loginWithPin_PIN불일치시_카운터증가() {
        User user = createUser(1L);
        setField(user, "pinHash", "encoded-pin");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("pin-fail:01012345678")).willReturn("2");
        given(userRepository.findByPhoneLast4("5678")).willReturn(List.of(user));
        given(pinService.verifyPin("wrong", "encoded-pin")).willReturn(false);
        given(valueOperations.increment("pin-fail:01012345678")).willReturn(3L);

        assertThatThrownBy(() -> authService.loginWithPin("01012345678", "wrong"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED));

        then(valueOperations).should().increment("pin-fail:01012345678");
        then(redisTemplate).should().expire(eq("pin-fail:01012345678"), eq(5L), any());
    }

    // ───────────── reissue ─────────────

    @Test
    void reissue_유효한refreshToken_새토큰반환() {
        given(jwtTokenProvider.validateToken("old-rt")).willReturn(true);
        given(jwtTokenProvider.getUserId("old-rt")).willReturn(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("RT:1")).willReturn("old-rt");
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("new-at");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("new-rt");
        given(jwtTokenProvider.getRefreshExpiration()).willReturn(604800000L);

        LoginResponse response = authService.reissue("old-rt");

        assertThat(response.accessToken()).isEqualTo("new-at");
        assertThat(response.refreshToken()).isEqualTo("new-rt");
    }

    @Test
    void reissue_유효하지않은토큰_예외발생() {
        given(jwtTokenProvider.validateToken("invalid-rt")).willReturn(false);

        assertThatThrownBy(() -> authService.reissue("invalid-rt"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID));
    }

    // ───────────── logout ─────────────

    @Test
    void logout_유효한accessToken_RT삭제_BL등록() {
        given(jwtTokenProvider.validateToken("at")).willReturn(true);
        given(jwtTokenProvider.getUserId("at")).willReturn(1L);
        given(jwtTokenProvider.getRemainingExpiration("at")).willReturn(900000L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        authService.logout("at");

        then(redisTemplate).should().delete("RT:1");
        then(valueOperations).should().set(eq("BL:at"), eq("logout"), anyLong(), any());
    }

    @Test
    void logout_유효하지않은토큰_예외발생() {
        given(jwtTokenProvider.validateToken("invalid-at")).willReturn(false);

        assertThatThrownBy(() -> authService.logout("invalid-at"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    // ───────────── testLogin ─────────────

    @Test
    void testLogin_신규_기본식별자() {
        given(userRepository.findByCi("test-ci-default")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User u = invocation.getArgument(0);
            setField(u, "id", 1L);
            return u;
        });
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshExpiration()).willReturn(604800000L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        LoginResponse response = authService.testLogin(null);

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    // ───────────── helper ─────────────

    private User createUser(Long id) {
        User user = User.builder()
                .ci("ci-hash")
                .di("di-hash")
                .name("홍길동")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("M")
                .phoneNumber("01012345678")
                .phoneLast4("5678")
                .build();
        setField(user, "id", id);
        return user;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
