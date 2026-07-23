package com.ssafy.tax7i.payment.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.auth.service.PinService;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PayPinServiceTest {

    private static final String FAIL_KEY = "pay-pin-fail:1";

    @Mock private UserRepository userRepository;
    @Mock private PinService pinService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PayPinService payPinService;

    // ───────────── setPayPin ─────────────

    @Test
    void setPayPin_해시저장() {
        User user = newUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(pinService.hashPin("123456")).willReturn("encoded-pay-pin");

        payPinService.setPayPin(1L, "123456");

        assertThat(user.getPayPinHash()).isEqualTo("encoded-pay-pin");
    }

    @Test
    void setPayPin_유저없음_예외() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> payPinService.setPayPin(99L, "123456"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    // ───────────── hasPayPin ─────────────

    @Test
    void hasPayPin_설정됨_true() {
        User user = newUser();
        setField(user, "payPinHash", "encoded-pay-pin");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThat(payPinService.hasPayPin(1L)).isTrue();
    }

    @Test
    void hasPayPin_미설정_false() {
        User user = newUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThat(payPinService.hasPayPin(1L)).isFalse();
    }

    // ───────────── verifyPayPin ─────────────

    @Test
    void verifyPayPin_일치_통과_실패카운터초기화() {
        User user = newUser();
        setField(user, "payPinHash", "encoded-pay-pin");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(FAIL_KEY)).willReturn(null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(pinService.verifyPin("123456", "encoded-pay-pin")).willReturn(true);

        assertThatCode(() -> payPinService.verifyPayPin(1L, "123456")).doesNotThrowAnyException();

        then(redisTemplate).should().delete(FAIL_KEY);
    }

    @Test
    void verifyPayPin_불일치_예외_실패카운터증가() {
        User user = newUser();
        setField(user, "payPinHash", "encoded-pay-pin");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(FAIL_KEY)).willReturn(null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(pinService.verifyPin("000000", "encoded-pay-pin")).willReturn(false);
        given(valueOperations.increment(FAIL_KEY)).willReturn(1L);

        assertThatThrownBy(() -> payPinService.verifyPayPin(1L, "000000"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PIN_INVALID));

        then(valueOperations).should().increment(FAIL_KEY);
    }

    @Test
    void verifyPayPin_시도횟수초과_예외() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(FAIL_KEY)).willReturn("5");

        assertThatThrownBy(() -> payPinService.verifyPayPin(1L, "123456"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PIN_ATTEMPTS_EXCEEDED));

        // 잠금 상태에서는 사용자 조회·검증까지 가지 않는다.
        then(userRepository).shouldHaveNoInteractions();
    }

    @Test
    void verifyPayPin_미설정_예외() {
        User user = newUser();
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(FAIL_KEY)).willReturn(null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> payPinService.verifyPayPin(1L, "123456"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PIN_NOT_SET));
    }

    // ───────────── helpers ─────────────

    private User newUser() {
        return User.builder()
                .ci("ci-hash")
                .di("di-hash")
                .name("홍길동")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("M")
                .phoneNumber("01012345678")
                .phoneLast4("5678")
                .build();
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
