package com.ssafy.tax7i.payment.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyCreditCardClient;
import com.ssafy.tax7i.banking.client.dto.*;
import com.ssafy.tax7i.bookentry.service.BookEntryService;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.card.service.CardTransactionSaveService;
import com.ssafy.tax7i.fcm.service.FcmService;
import com.ssafy.tax7i.classification.service.TaxClassificationService;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.payment.dto.*;
import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.entity.PaymentMethod;
import com.ssafy.tax7i.payment.entity.PaymentPurpose;
import com.ssafy.tax7i.payment.entity.PaymentStatus;
import com.ssafy.tax7i.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private CardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private SsafyCreditCardClient ssafyCreditCardClient;
    @Mock private BookEntryService bookEntryService;
    @Mock private TaxClassificationService taxClassificationService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CardTransactionSaveService cardTransactionSaveService;
    @Mock private FcmService fcmService;

    @InjectMocks
    private PaymentService paymentService;

    // ───────────── authorize ─────────────

    @Test
    void authorize_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(card));
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            setField(p, "id", 1L);
            return p;
        });

        PaymentAuthorizeRequest request = new PaymentAuthorizeRequest(
                1L, 10000L, "KRW", 1L, "스타벅스", "5812", PaymentMethod.OFFLINE, PaymentPurpose.BUSINESS);

        PaymentAuthorizeResponse response = paymentService.authorize(1L, request);

        assertThat(response.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(response.amount()).isEqualTo(10000L);
    }

    @Test
    void authorize_카드없음_예외() {
        User user = createUser(1L, "user-key");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(99L, 1L)).willReturn(Optional.empty());

        PaymentAuthorizeRequest request = new PaymentAuthorizeRequest(
                99L, 10000L, null, 1L, "테스트", null, PaymentMethod.ONLINE, PaymentPurpose.PERSONAL);

        assertThatThrownBy(() -> paymentService.authorize(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CARD_NOT_FOUND));
    }

    @Test
    void authorize_userId_null이면_UNAUTHORIZED_예외() {
        PaymentAuthorizeRequest request = new PaymentAuthorizeRequest(
                1L, 10000L, null, 1L, "스타벅스", null, PaymentMethod.OFFLINE, PaymentPurpose.PERSONAL);

        assertThatThrownBy(() -> paymentService.authorize(null, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    // ───────────── capture ─────────────

    @Test
    void capture_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.AUTHORIZED);

        given(paymentRepository.findByIdAndUserIdWithFetch(1L, 1L)).willReturn(Optional.of(payment));
        given(ssafyCreditCardClient.createTransaction("user-key", "1005518816096479", "725", 1L, 10000L))
                .willReturn(createTransactionResponse(100L));

        PaymentCaptureResponse response = paymentService.capture(1L, 1L);

        assertThat(response.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(response.amount()).isEqualTo(10000L);
        verify(cardTransactionSaveService).save(any(), any(), any(), any(), any());
    }

    @Test
    void capture_승인상태아님_예외() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.CAPTURED);

        given(paymentRepository.findByIdAndUserIdWithFetch(1L, 1L)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.capture(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_DECLINED));
    }

    @Test
    void capture_결제없음_예외() {
        given(paymentRepository.findByIdAndUserIdWithFetch(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.capture(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));
    }

    // ───────────── cancel ─────────────

    @Test
    void cancel_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.CAPTURED);
        setField(payment, "ssafyTransactionUniqueNo", 100L);

        given(paymentRepository.findByIdAndUserIdWithFetch(1L, 1L)).willReturn(Optional.of(payment));
        given(ssafyCreditCardClient.deleteTransaction("user-key", "1005518816096479", "725", 100L))
                .willReturn(new SsafyDeleteCreditCardTransactionResponse(
                        successHeader(),
                        new SsafyDeleteCreditCardTransactionResponse.DeletedTransactionRec(100L, "CANCELLED")));

        PaymentCancelRequest request = new PaymentCancelRequest(10000L, "고객 요청");
        PaymentCancelResponse response = paymentService.cancel(1L, 1L, request);

        assertThat(response.status()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(response.cancelledAmount()).isEqualTo(10000L);
        verify(cardTransactionSaveService).save(any(), any(), any(), any(), any());
    }

    @Test
    void cancel_확정되지않은결제_예외() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.AUTHORIZED);

        given(paymentRepository.findByIdAndUserIdWithFetch(1L, 1L)).willReturn(Optional.of(payment));

        PaymentCancelRequest request = new PaymentCancelRequest(10000L, "취소 사유");

        assertThatThrownBy(() -> paymentService.cancel(1L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_DECLINED));
    }

    // ───────────── getPayment ─────────────

    @Test
    void getPayment_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        Payment payment = createPayment(1L, user, card, 15000L, PaymentStatus.CAPTURED);

        given(paymentRepository.findByIdAndUserIdWithFetch(1L, 1L)).willReturn(Optional.of(payment));

        PaymentDetailResponse response = paymentService.getPayment(1L, 1L);

        assertThat(response.amount()).isEqualTo(15000L);
        assertThat(response.status()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    void getPayment_결제없음_예외() {
        given(paymentRepository.findByIdAndUserIdWithFetch(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));
    }

    // ───────────── processQrPayment ─────────────

    @Test
    void processQrPayment_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(card));
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            setField(p, "id", 1L);
            return p;
        });
        given(ssafyCreditCardClient.createTransaction("user-key", "1005518816096479", "725", 1L, 10000L))
                .willReturn(createTransactionResponse(200L));

        QrPaymentRequest request = new QrPaymentRequest(1L, 10000L, 1L, "스타벅스", "5812", PaymentPurpose.BUSINESS);

        QrPaymentResponse response = paymentService.processQrPayment(1L, request);

        assertThat(response.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(response.amount()).isEqualTo(10000L);
        verify(cardTransactionSaveService).save(any(), any(), any(), any(), any());
    }

    @Test
    void processQrPayment_결제실패_예외() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(card));
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            setField(p, "id", 1L);
            return p;
        });
        given(ssafyCreditCardClient.createTransaction("user-key", "1005518816096479", "725", 1L, 10000L))
                .willThrow(new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE));

        QrPaymentRequest request = new QrPaymentRequest(1L, 10000L, 1L, "스타벅스", "5812", PaymentPurpose.BUSINESS);

        assertThatThrownBy(() -> paymentService.processQrPayment(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BANK_SERVICE_UNAVAILABLE));
    }

    // ───────────── getPayments ─────────────

    @Test
    void getPayments_날짜필터없음() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.CAPTURED);
        Page<Payment> page = new PageImpl<>(List.of(payment));

        given(paymentRepository.findByUserIdWithFetch(eq(1L), any())).willReturn(page);

        Page<PaymentDetailResponse> result = paymentService.getPayments(1L, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).amount()).isEqualTo(10000L);
    }

    @Test
    void getPayments_날짜필터() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        Payment payment = createPayment(1L, user, card, 20000L, PaymentStatus.CAPTURED);
        Page<Payment> page = new PageImpl<>(List.of(payment));

        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        given(paymentRepository.findByUserIdAndCapturedAtBetweenWithFetch(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .willReturn(page);

        Page<PaymentDetailResponse> result = paymentService.getPayments(1L, start, end, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).amount()).isEqualTo(20000L);
    }

    @Test
    void getPayments_상태필터() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        Payment payment = createPayment(1L, user, card, 15000L, PaymentStatus.CANCELLED);
        Page<Payment> page = new PageImpl<>(List.of(payment));

        given(paymentRepository.findByUserIdAndStatus(eq(1L), eq(PaymentStatus.CANCELLED), any()))
                .willReturn(page);

        Page<PaymentDetailResponse> result = paymentService.getPayments(
                1L, null, null, PaymentStatus.CANCELLED, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(PaymentStatus.CANCELLED);
    }

    // ───────────── QR 토큰 결제 ─────────────

    @Test
    void createQrToken_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(card));
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            setField(p, "id", 1L);
            return p;
        });
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        QrTokenCreateRequest request = new QrTokenCreateRequest(
                1L, 10000L, 1L, "스타벅스", "5812", PaymentPurpose.BUSINESS);

        QrTokenCreateResponse response = paymentService.createQrToken(1L, request);

        assertThat(response.paymentId()).isEqualTo(1L);
        assertThat(response.amount()).isEqualTo(10000L);
        assertThat(response.payerName()).isEqualTo("홍길동");
        assertThat(response.token()).isNotNull();
        assertThat(response.expiresAt()).isNotNull();
    }

    @Test
    void getQrPaymentInfo_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.AUTHORIZED);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("qr-pay:test-token")).willReturn("1");
        given(paymentRepository.findByIdWithFetch(1L)).willReturn(Optional.of(payment));

        QrPaymentInfoResponse response = paymentService.getQrPaymentInfo("test-token");

        assertThat(response.payerName()).isEqualTo("홍길동");
        assertThat(response.amount()).isEqualTo(10000L);
        assertThat(response.status()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    void getQrPaymentInfo_토큰만료_예외() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("qr-pay:expired-token")).willReturn(null);

        assertThatThrownBy(() -> paymentService.getQrPaymentInfo("expired-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.QR_TOKEN_EXPIRED));
    }

    @Test
    void confirmQrPayment_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.AUTHORIZED);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("qr-pay:test-token")).willReturn("1");
        given(valueOperations.getAndDelete("qr-pay:test-token")).willReturn("1");
        given(paymentRepository.findByIdWithFetchForUpdate(1L)).willReturn(Optional.of(payment));
        given(ssafyCreditCardClient.createTransaction("user-key", "1005518816096479", "725", 1L, 10000L))
                .willReturn(createTransactionResponse(300L));

        QrPaymentResponse response = paymentService.confirmQrPayment("test-token");

        assertThat(response.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(response.amount()).isEqualTo(10000L);
        verify(cardTransactionSaveService).save(any(), any(), any(), any(), any());
    }

    @Test
    void confirmQrPayment_이미처리된결제_예외() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        Payment payment = createPayment(1L, user, card, 10000L, PaymentStatus.CAPTURED);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("qr-pay:test-token")).willReturn("1");
        given(paymentRepository.findByIdWithFetchForUpdate(1L)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmQrPayment("test-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_DECLINED));
    }

    // ───────────── helpers ─────────────

    private User createUser(Long id, String userKey) {
        User user = User.builder()
                .ci("ci-hash")
                .di("di-hash")
                .name("홍길동")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("M")
                .phoneNumber("01012345678")
                .phoneLast4("5678")
                .ssafyUserKey(userKey)
                .build();
        setField(user, "id", id);
        return user;
    }

    private Card createCard(Long id, User user) {
        Card card = Card.builder()
                .user(user)
                .cardName("테스트 카드")
                .cardType(CardType.BUSINESS)
                .last4Digits("6479")
                .cardNo("1005518816096479")
                .cvc("725")
                .cardUniqueNo("1003-xxx")
                .ssafyAccountNo("0123456789012345")
                .withdrawalAccountNo("0123456789012345")
                .withdrawalDate("4")
                .cardExpiryDate("20290401")
                .build();
        setField(card, "id", id);
        return card;
    }

    private Payment createPayment(Long id, User user, Card card, Long amount, PaymentStatus status) {
        Payment payment = Payment.builder()
                .user(user)
                .card(card)
                .amount(amount)
                .currency("KRW")
                .merchantId(1L)
                .merchantName("스타벅스")
                .merchantCategoryCode("5812")
                .paymentMethod(PaymentMethod.OFFLINE)
                .purpose(PaymentPurpose.BUSINESS)
                .authorizationCode("AUTH1234")
                .build();
        setField(payment, "id", id);
        setField(payment, "status", status);
        return payment;
    }

    private SsafyCreditCardTransactionResponse createTransactionResponse(Long txUniqueNo) {
        return new SsafyCreditCardTransactionResponse(
                successHeader(),
                new SsafyCreditCardTransactionResponse.TransactionRec(
                        txUniqueNo, "1005518816096479", 1L, "스타벅스", "CG-001", "카페",
                        10000L, "20260320", "120000", "COMPLETED"
                )
        );
    }

    private SsafyResponseHeader successHeader() {
        return new SsafyResponseHeader("H0000", "정상처리 되었습니다.", "test", "20260320", "120000", "00100", "key", "test", "uniqueNo");
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
