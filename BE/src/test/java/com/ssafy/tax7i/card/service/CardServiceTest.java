package com.ssafy.tax7i.card.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyCreditCardClient;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.*;
import com.ssafy.tax7i.card.dto.*;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.sms.service.SmsOtpService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private SsafyCreditCardClient ssafyCreditCardClient;
    @Mock private SsafyFinanceClient ssafyFinanceClient;
    @Mock private SmsOtpService smsOtpService;

    @InjectMocks
    private CardService cardService;

    // ───────────── createCard ─────────────

    @Test
    void createCard_성공() {
        User user = createUser(1L, "user-key");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(ssafyCreditCardClient.createCreditCard("user-key", "1003-xxx", "0123456789012345", "4"))
                .willReturn(createCreditCardResponse());
        given(cardRepository.save(any(Card.class))).willAnswer(invocation -> {
            Card c = invocation.getArgument(0);
            setField(c, "id", 1L);
            return c;
        });

        CreateCardRequest request = new CreateCardRequest("사업용 카드", CardType.BUSINESS, "1003-xxx", "0123456789012345", "4", "test-otp-token");
        CardResponse response = cardService.createCard(1L, request);

        assertThat(response.cardName()).isEqualTo("사업용 카드");
        assertThat(response.cardType()).isEqualTo(CardType.BUSINESS);
        assertThat(response.last4Digits()).isEqualTo("6479");
    }

    // ───────────── setDefaultCard ─────────────

    @Test
    void setDefaultCard_성공() {
        User user = createUser(1L, "user-key");
        Card oldDefault = createCard(2L, user);
        setField(oldDefault, "isDefault", true);
        Card newDefault = createCard(1L, user);

        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(newDefault));
        given(cardRepository.findByUser_IdAndIsDefaultTrueAndDeletedFalse(1L)).willReturn(Optional.of(oldDefault));

        CardResponse response = cardService.setDefaultCard(1L, 1L);

        assertThat(response.isDefault()).isTrue();
        assertThat(oldDefault.isDefault()).isFalse();
    }

    // ───────────── payment ─────────────

    @Test
    void payment_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(card));
        given(ssafyCreditCardClient.createTransaction("user-key", "1005518816096479", "725", 1L, 50000L))
                .willReturn(createTransactionResponse());

        CardPaymentRequest request = new CardPaymentRequest(1L, 50000L);
        CardPaymentResponse response = cardService.payment(1L, 1L, request);

        assertThat(response.paymentBalance()).isEqualTo(50000L);
        assertThat(response.cardId()).isEqualTo(1L);
    }

    // ───────────── getTransactions ─────────────

    @Test
    void getTransactions_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(card));
        given(ssafyCreditCardClient.getTransactionHistory("user-key", "1005518816096479", "725", "20260301", "20260331"))
                .willReturn(createTransactionListResponse());

        List<CardTransactionResponse> response = cardService.getTransactions(1L, 1L, "20260301", "20260331");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).paymentBalance()).isEqualTo(50000L);
    }

    // ───────────── getCardWithOwnership ─────────────

    @Test
    void getCard_소유권불일치_예외() {
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCard(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CARD_NOT_FOUND));
    }

    // ───────────── deleteCard ─────────────

    @Test
    void deleteCard_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);

        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(card));

        cardService.deleteCard(1L, 1L);

        assertThat(card.isDeleted()).isTrue();
    }

    // ───────────── activateCard ─────────────

    @Test
    void activateCard_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        // card.status is INACTIVE by default (from builder)

        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(card));

        CardActivateRequest request = new CardActivateRequest("1234");
        CardActivateResponse response = cardService.activateCard(1L, 1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(card.getStatus()).isEqualTo(com.ssafy.tax7i.card.entity.CardStatus.ACTIVE);
    }

    @Test
    void activateCard_이미활성_예외() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);
        card.activate(); // force ACTIVE status

        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(card));

        CardActivateRequest request = new CardActivateRequest("1234");
        assertThatThrownBy(() -> cardService.activateCard(1L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void activateCard_소유권불일치_예외() {
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(99L, 1L)).willReturn(Optional.empty());

        CardActivateRequest request = new CardActivateRequest("1234");
        assertThatThrownBy(() -> cardService.activateCard(1L, 99L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CARD_NOT_FOUND));
    }

    // ───────────── setCardPurpose ─────────────

    @Test
    void setCardPurpose_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user);

        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(card));

        CardPurposeRequest request = new CardPurposeRequest("BUSINESS");
        CardPurposeResponse response = cardService.setCardPurpose(1L, 1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.defaultPurpose()).isEqualTo("BUSINESS");
        assertThat(card.getDefaultPurpose()).isEqualTo("BUSINESS");
    }

    @Test
    void setCardPurpose_소유권불일치_예외() {
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(99L, 1L)).willReturn(Optional.empty());

        CardPurposeRequest request = new CardPurposeRequest("BUSINESS");
        assertThatThrownBy(() -> cardService.setCardPurpose(1L, 99L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CARD_NOT_FOUND));
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

    private SsafyCreateCreditCardResponse createCreditCardResponse() {
        return new SsafyCreateCreditCardResponse(
                successHeader(),
                new SsafyCreateCreditCardResponse.CreditCardRec(
                        "1005518816096479", "725", "1003-xxx", "1003", "삼성카드",
                        "테스트 카드", "700000", "130000", "테스트 카드 설명",
                        "20290401", "0123456789012345", "4"
                )
        );
    }

    private SsafyCreditCardTransactionResponse createTransactionResponse() {
        return new SsafyCreditCardTransactionResponse(
                successHeader(),
                new SsafyCreditCardTransactionResponse.TransactionRec(
                        1L, "1005518816096479", 1L, "스타벅스", "CG-001", "카페",
                        50000L, "20260320", "120000", "COMPLETED"
                )
        );
    }

    private SsafyCreditCardTransactionListResponse createTransactionListResponse() {
        return new SsafyCreditCardTransactionListResponse(
                successHeader(),
                List.of(new SsafyCreditCardTransactionListResponse.TransactionRec(
                        1L, "20260320", "120000", "1005518816096479", 1L, "스타벅스",
                        "CG-001", "카페", 50000L, "COMPLETED"
                ))
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
