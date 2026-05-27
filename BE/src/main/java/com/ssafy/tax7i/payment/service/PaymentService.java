package com.ssafy.tax7i.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyCreditCardClient;
import com.ssafy.tax7i.banking.client.dto.SsafyCreditCardTransactionResponse;
import com.ssafy.tax7i.bookentry.event.PaymentCapturedEvent;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardTransactionType;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.card.service.CardTransactionSaveService;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.payment.dto.*;
import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.entity.PaymentMethod;
import com.ssafy.tax7i.payment.entity.PaymentPurpose;
import com.ssafy.tax7i.payment.entity.PaymentStatus;
import com.ssafy.tax7i.payment.event.PaymentCancelledEvent;
import com.ssafy.tax7i.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final String QR_TOKEN_PREFIX = "qr-pay:";
    private static final String MERCHANT_QR_PREFIX = "qr-merchant:";
    private static final long QR_TOKEN_TTL_SECONDS = 300; // 5분

    private final PaymentRepository paymentRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final SsafyCreditCardClient ssafyCreditCardClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final CardTransactionSaveService cardTransactionSaveService;
    private final ObjectMapper objectMapper;

    private final Map<String, SseEmitter> qrEmitters = new ConcurrentHashMap<>();

    private record PaymentContext(User user, Card card, String userKey, String authCode) {}

    private PaymentContext preparePayment(Long userId, Long cardId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Card card = cardRepository.findByIdAndUser_IdAndDeletedFalse(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        String userKey = getUserKey(user);
        String authCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentContext(user, card, userKey, authCode);
    }

    @Transactional
    public PaymentAuthorizeResponse authorize(Long userId, PaymentAuthorizeRequest request) {
        PaymentContext ctx = preparePayment(userId, request.cardId());

        Payment payment = Payment.builder()
                .user(ctx.user())
                .card(ctx.card())
                .amount(request.amount())
                .currency(request.currencyOrDefault())
                .merchantId(request.merchantId())
                .merchantName(request.merchantName())
                .merchantCategoryCode(request.merchantCategoryCode())
                .paymentMethod(request.paymentMethod())
                .purpose(request.purpose())
                .authorizationCode(ctx.authCode())
                .build();

        paymentRepository.save(payment);
        return PaymentAuthorizeResponse.from(payment);
    }

    @Transactional
    public PaymentCaptureResponse capture(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndUserIdWithFetch(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new BusinessException(ErrorCode.PAYMENT_DECLINED, "승인 상태의 결제만 확정할 수 있습니다.");
        }

        Card card = payment.getCard();
        User user = payment.getUser();
        String userKey = getUserKey(user);

        SsafyCreditCardTransactionResponse transactionResponse;
        try {
            transactionResponse = ssafyCreditCardClient.createTransaction(
                    userKey, card.getCardNo(), card.getCvc(),
                    payment.getMerchantId(), payment.getAmount());
        } catch (BusinessException e) {
            payment.decline();
            throw e;
        }

        payment.capture();
        payment.assignSsafyTransaction(transactionResponse.rec().transactionUniqueNo());
        saveCardTransaction(card, CardTransactionType.PAYMENT, payment.getAmount(),
                "결제: " + payment.getMerchantName(), transactionResponse.rec().paymentBalance());

        publishPaymentCapturedEvent(payment);

        return PaymentCaptureResponse.of(payment);
    }

    @Transactional
    public PaymentCancelResponse cancel(Long userId, Long paymentId, PaymentCancelRequest request) {
        Payment payment = paymentRepository.findByIdAndUserIdWithFetch(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new BusinessException(ErrorCode.PAYMENT_DECLINED, "확정된 결제만 취소할 수 있습니다.");
        }

        long cancelAmount = request.cancelAmount() != null ? request.cancelAmount() : payment.getAmount();
        long alreadyCancelled = payment.getCancelledAmount() != null ? payment.getCancelledAmount() : 0L;
        if (cancelAmount > payment.getAmount() - alreadyCancelled) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "취소 금액이 남은 결제 금액을 초과할 수 없습니다.");
        }

        Card card = payment.getCard();
        User user = payment.getUser();
        String userKey = getUserKey(user);

        if (payment.getSsafyTransactionUniqueNo() != null) {
            ssafyCreditCardClient.deleteTransaction(
                    userKey, card.getCardNo(), card.getCvc(), payment.getSsafyTransactionUniqueNo());
        }

        payment.cancel(cancelAmount, request.reason());
        saveCardTransaction(card, CardTransactionType.REFUND, cancelAmount,
                "환불: " + payment.getMerchantName(), null);
        eventPublisher.publishEvent(new PaymentCancelledEvent(
                payment.getId(),
                payment.getUser().getId(),
                payment.getAmount(),
                payment.getMerchantName()
        ));

        return PaymentCancelResponse.from(payment);
    }

    public PaymentDetailResponse getPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndUserIdWithFetch(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        return PaymentDetailResponse.from(payment);
    }

    public Page<PaymentDetailResponse> getPayments(Long userId, LocalDate startDate, LocalDate endDate,
                                                    PaymentStatus status, Pageable pageable) {
        boolean hasDateRange = startDate != null && endDate != null;

        if (hasDateRange) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            if (status != null) {
                return paymentRepository.findByUserIdAndCapturedAtBetweenAndStatusWithFetch(userId, start, end, status, pageable)
                        .map(PaymentDetailResponse::from);
            }
            return paymentRepository.findByUserIdAndCapturedAtBetweenWithFetch(userId, start, end, pageable)
                    .map(PaymentDetailResponse::from);
        }
        if (status != null) {
            return paymentRepository.findByUserIdAndStatus(userId, status, pageable)
                    .map(PaymentDetailResponse::from);
        }
        return paymentRepository.findByUserIdWithFetch(userId, pageable)
                .map(PaymentDetailResponse::from);
    }

    @Transactional
    public QrPaymentResponse processQrPayment(Long userId, QrPaymentRequest request) {
        PaymentContext ctx = preparePayment(userId, request.cardId());

        Payment payment = Payment.builder()
                .user(ctx.user())
                .card(ctx.card())
                .amount(request.amount())
                .currency("KRW")
                .merchantId(request.merchantId())
                .merchantName(request.merchantName())
                .merchantCategoryCode(request.merchantCategoryCode())
                .paymentMethod(PaymentMethod.OFFLINE)
                .purpose(request.purpose())
                .authorizationCode(ctx.authCode())
                .build();

        paymentRepository.save(payment);

        SsafyCreditCardTransactionResponse transactionResponse;
        try {
            transactionResponse = ssafyCreditCardClient.createTransaction(
                    ctx.userKey(), ctx.card().getCardNo(), ctx.card().getCvc(),
                    request.merchantId(), request.amount());
        } catch (BusinessException e) {
            payment.decline();
            throw e;
        }

        payment.capture();
        payment.assignSsafyTransaction(transactionResponse.rec().transactionUniqueNo());
        saveCardTransaction(ctx.card(), CardTransactionType.PAYMENT, request.amount(),
                "결제: " + request.merchantName(), transactionResponse.rec().paymentBalance());

        publishPaymentCapturedEvent(payment);

        return QrPaymentResponse.of(payment);
    }

    // ───────────── QR 토큰 결제 (MPM) ─────────────

    @Transactional
    public QrTokenCreateResponse createQrToken(Long userId, QrTokenCreateRequest request) {
        PaymentContext ctx = preparePayment(userId, request.cardId());

        Payment payment = Payment.builder()
                .user(ctx.user())
                .card(ctx.card())
                .amount(request.amount())
                .currency("KRW")
                .merchantId(request.merchantId())
                .merchantName(request.merchantName())
                .merchantCategoryCode(request.merchantCategoryCode())
                .paymentMethod(PaymentMethod.OFFLINE)
                .purpose(request.purpose())
                .authorizationCode(ctx.authCode())
                .build();
        paymentRepository.save(payment);

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                QR_TOKEN_PREFIX + token,
                String.valueOf(payment.getId()),
                QR_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(QR_TOKEN_TTL_SECONDS);
        return new QrTokenCreateResponse(token, payment.getId(), payment.getAmount(),
                ctx.user().getName(), expiresAt);
    }

    public QrPaymentInfoResponse getQrPaymentInfo(String token) {
        Long paymentId = peekPaymentIdFromToken(token);
        Payment payment = paymentRepository.findByIdWithFetch(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        return new QrPaymentInfoResponse(
                token,
                payment.getUser().getName(),
                payment.getAmount(),
                payment.getMerchantName(),
                payment.getPurpose(),
                payment.getStatus(),
                payment.getCreatedAt());
    }

    @Transactional
    public QrPaymentResponse confirmQrPayment(String token) {
        // peek → DB lock → 외부 API → consume 패턴:
        // 이론적 TOCTOU 윈도우가 존재하나, findByIdWithFetchForUpdate(PESSIMISTIC_WRITE)가
        // 동일 Payment에 대한 동시 처리를 직렬화하여 중복 결제를 방지함.
        // 토큰을 먼저 소비하지 않는 이유: 외부 API 실패 시 재시도 가능하도록.
        Long paymentId = peekPaymentIdFromToken(token);
        Payment payment = paymentRepository.findByIdWithFetchForUpdate(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new BusinessException(ErrorCode.PAYMENT_DECLINED, "대기 중인 결제만 확인할 수 있습니다.");
        }

        Card card = payment.getCard();
        User payer = payment.getUser();
        String userKey = getUserKey(payer);

        SsafyCreditCardTransactionResponse transactionResponse;
        try {
            transactionResponse = ssafyCreditCardClient.createTransaction(
                    userKey, card.getCardNo(), card.getCvc(),
                    payment.getMerchantId(), payment.getAmount());
        } catch (BusinessException e) {
            // 외부 API 실패 — 토큰을 삭제하지 않아 사용자가 재시도 가능
            payment.decline();
            notifyQrPaymentResult(token, payment);
            throw e;
        }

        // 외부 API 성공 후 토큰 소비 (중복 결제 방어)
        consumePaymentToken(token);

        payment.capture();
        payment.assignSsafyTransaction(transactionResponse.rec().transactionUniqueNo());
        saveCardTransaction(card, CardTransactionType.PAYMENT, payment.getAmount(),
                "결제: " + payment.getMerchantName(), transactionResponse.rec().paymentBalance());

        publishPaymentCapturedEvent(payment);

        notifyQrPaymentResult(token, payment);
        return QrPaymentResponse.of(payment);
    }

    public QrPaymentStatusResponse getQrPaymentStatus(Long userId, String token) {
        Long paymentId = peekPaymentIdFromToken(token);
        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        return new QrPaymentStatusResponse(token, payment.getStatus(),
                payment.getId(), payment.getCapturedAt());
    }

    public SseEmitter subscribeQrPayment(Long userId, String token) {
        Long paymentId = peekPaymentIdFromToken(token);
        paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        SseEmitter emitter = new SseEmitter(QR_TOKEN_TTL_SECONDS * 1000);
        qrEmitters.put(token, emitter);

        emitter.onCompletion(() -> qrEmitters.remove(token));
        emitter.onTimeout(() -> qrEmitters.remove(token));
        emitter.onError(e -> qrEmitters.remove(token));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception e) {
            log.debug("SSE 초기 이벤트 전송 실패: {}", e.getMessage());
        }

        return emitter;
    }

    // ───────────── 가맹점 QR 결제 (MPM) ─────────────

    public MerchantQrTokenResponse createMerchantQrToken(MerchantQrCreateRequest request) {
        String token = UUID.randomUUID().toString();

        Map<String, Object> merchantInfo = Map.of(
                "amount", request.amount(),
                "merchantId", request.merchantId(),
                "merchantName", request.merchantName(),
                "merchantCategoryCode", request.merchantCategoryCode() != null ? request.merchantCategoryCode() : "",
                "purpose", request.purpose().name()
        );

        try {
            String json = objectMapper.writeValueAsString(merchantInfo);
            redisTemplate.opsForValue().set(
                    MERCHANT_QR_PREFIX + token, json,
                    QR_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("가맹점 QR 토큰 생성 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "QR 토큰 생성 실패: " + e.getMessage());
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(QR_TOKEN_TTL_SECONDS);
        return new MerchantQrTokenResponse(token, request.amount(), request.merchantName(), expiresAt);
    }

    public MerchantQrInfoResponse getMerchantQrInfo(String token) {
        String json = redisTemplate.opsForValue().get(MERCHANT_QR_PREFIX + token);
        if (json == null) {
            throw new BusinessException(ErrorCode.QR_TOKEN_EXPIRED);
        }
        try {
            Map<String, Object> info = objectMapper.readValue(json, new TypeReference<>() {});
            return new MerchantQrInfoResponse(
                    token,
                    ((Number) info.get("amount")).longValue(),
                    (String) info.get("merchantName"),
                    (String) info.get("merchantCategoryCode"),
                    PaymentPurpose.valueOf((String) info.get("purpose"))
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "QR 정보 조회 실패");
        }
    }

    @Transactional
    public QrPaymentResponse payMerchantQr(Long userId, String token, MerchantQrPayRequest request) {
        // 1. Get and delete merchant info from Redis (atomic consumption)
        String json = redisTemplate.opsForValue().getAndDelete(MERCHANT_QR_PREFIX + token);
        if (json == null) {
            throw new BusinessException(ErrorCode.QR_TOKEN_EXPIRED);
        }

        Map<String, Object> info;
        try {
            info = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "QR 정보 파싱 실패");
        }

        // 2. Prepare payment context (validates user + card ownership)
        PaymentContext ctx = preparePayment(userId, request.cardId());

        Long amount = ((Number) info.get("amount")).longValue();
        Long merchantId = ((Number) info.get("merchantId")).longValue();
        String merchantName = (String) info.get("merchantName");
        String mcc = (String) info.get("merchantCategoryCode");
        PaymentPurpose purpose = PaymentPurpose.valueOf((String) info.get("purpose"));

        // 3. Create Payment entity (with user + card — same as existing flow)
        Payment payment = Payment.builder()
                .user(ctx.user())
                .card(ctx.card())
                .amount(amount)
                .currency("KRW")
                .merchantId(merchantId)
                .merchantName(merchantName)
                .merchantCategoryCode(mcc)
                .paymentMethod(PaymentMethod.OFFLINE)
                .purpose(purpose)
                .authorizationCode(ctx.authCode())
                .build();
        paymentRepository.save(payment);

        // 4. Call SSAFY API
        SsafyCreditCardTransactionResponse transactionResponse;
        try {
            transactionResponse = ssafyCreditCardClient.createTransaction(
                    ctx.userKey(), ctx.card().getCardNo(), ctx.card().getCvc(),
                    merchantId, amount);
        } catch (BusinessException e) {
            payment.decline();
            throw e;
        }

        // 5. Capture + events (same as existing flow)
        payment.capture();
        payment.assignSsafyTransaction(transactionResponse.rec().transactionUniqueNo());
        saveCardTransaction(ctx.card(), CardTransactionType.PAYMENT, amount,
                "결제: " + merchantName, transactionResponse.rec().paymentBalance());

        publishPaymentCapturedEvent(payment);

        // 6. Notify merchant via SSE
        notifyQrPaymentResult(token, payment);

        return QrPaymentResponse.of(payment);
    }

    public SseEmitter subscribeMerchantQrPayment(String token) {
        String json = redisTemplate.opsForValue().get(MERCHANT_QR_PREFIX + token);
        if (json == null) {
            throw new BusinessException(ErrorCode.QR_TOKEN_EXPIRED);
        }

        SseEmitter emitter = new SseEmitter(QR_TOKEN_TTL_SECONDS * 1000L);
        qrEmitters.put(token, emitter);

        emitter.onCompletion(() -> qrEmitters.remove(token));
        emitter.onTimeout(() -> qrEmitters.remove(token));
        emitter.onError(e -> qrEmitters.remove(token));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception e) {
            log.debug("SSE 초기 이벤트 전송 실패: {}", e.getMessage());
        }

        return emitter;
    }

    private void notifyQrPaymentResult(String token, Payment payment) {
        SseEmitter emitter = qrEmitters.remove(token);
        if (emitter != null) {
            try {
                QrPaymentStatusResponse status = new QrPaymentStatusResponse(
                        token, payment.getStatus(), payment.getId(), payment.getCapturedAt());
                emitter.send(SseEmitter.event().name("qr-payment-result").data(status));
                emitter.complete();
            } catch (Exception e) {
                log.debug("QR 결제 SSE 전송 실패 (클라이언트 연결 끊김): {}", e.getMessage());
            }
        }
    }

    /** 토큰 조회 (읽기 전용 — 정보 조회, 상태 확인, SSE 구독용) */
    private Long peekPaymentIdFromToken(String token) {
        String paymentIdStr = redisTemplate.opsForValue().get(QR_TOKEN_PREFIX + token);
        if (paymentIdStr == null) {
            throw new BusinessException(ErrorCode.QR_TOKEN_EXPIRED);
        }
        return Long.parseLong(paymentIdStr);
    }

    /** 토큰 소비 (원자적 조회+삭제 — 결제 확정 전용, 중복 결제 방어) */
    private Long consumePaymentToken(String token) {
        String paymentIdStr = redisTemplate.opsForValue().getAndDelete(QR_TOKEN_PREFIX + token);
        if (paymentIdStr == null) {
            throw new BusinessException(ErrorCode.QR_TOKEN_EXPIRED);
        }
        return Long.parseLong(paymentIdStr);
    }

    private void publishPaymentCapturedEvent(Payment payment) {
        eventPublisher.publishEvent(new PaymentCapturedEvent(
                payment.getId(),
                payment.getUser().getId(),
                payment.getAmount(),
                payment.getMerchantName(),
                payment.getMerchantCategoryCode(),
                payment.getCard().getCardType().name(),
                payment.getCapturedAt()
        ));
    }

    private void saveCardTransaction(Card card, CardTransactionType type, Long amount, String description, Long paymentBalance) {
        try {
            cardTransactionSaveService.save(card, type, amount, description, paymentBalance);
        } catch (Exception e) {
            log.warn("카드 거래 기록 저장 실패 (결제는 정상): cardId={}, error={}", card.getId(), e.getMessage());
        }
    }

    private String getUserKey(User user) {
        if (user.getSsafyUserKey() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "SSAFY 금융망 사용자 키가 등록되지 않았습니다.");
        }
        return user.getSsafyUserKey();
    }
}
