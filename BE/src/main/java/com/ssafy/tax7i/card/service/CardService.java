package com.ssafy.tax7i.card.service;

import com.ssafy.tax7i.banking.client.SsafyCreditCardClient;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.*;
import com.ssafy.tax7i.card.dto.*;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardStatus;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.sms.service.SmsOtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final SsafyCreditCardClient ssafyCreditCardClient;
    private final SsafyFinanceClient ssafyFinanceClient;
    private final SmsOtpService smsOtpService;

    /**
     * 가맹점 목록 조회
     */
    public List<MerchantResponse> getMerchants(Long userId) {
        User user = getUser(userId);
        String userKey = getUserKey(user);

        SsafyMerchantListResponse response = ssafyCreditCardClient.getMerchantList(userKey);
        if (response.rec() == null) return List.of();
        return response.rec().stream()
                .map(MerchantResponse::from)
                .toList();
    }

    /**
     * 내 계좌 목록 조회
     */
    public List<AccountResponse> getMyAccounts(Long userId) {
        User user = getUser(userId);
        String userKey = getUserKey(user);

        SsafyAccountListResponse response = ssafyFinanceClient.getAccountList(userKey);
        if (response.rec() == null) return List.of();
        return response.rec().stream()
                .map(r -> new AccountResponse(r.accountNo(), r.accountName(), r.accountBalance()))
                .toList();
    }

    /**
     * 카드 상품 목록 조회
     */
    public List<CardProductResponse> getCardProducts(Long userId) {
        User user = getUser(userId);
        String userKey = getUserKey(user);

        SsafyCreditCardProductListResponse response = ssafyCreditCardClient.getCreditCardProducts(userKey);
        return response.rec().stream()
                .map(CardProductResponse::from)
                .toList();
    }

    /**
     * 신용카드 발급 (SSAFY API)
     */
    @Transactional
    public CardResponse createCard(Long userId, CreateCardRequest request) {
        // OTP 토큰 검증 (1회용 — 검증 후 토큰 삭제됨)
        smsOtpService.validateOtpToken(userId, request.otpToken());

        User user = getUser(userId);
        String userKey = getUserKey(user);

        SsafyCreateCreditCardResponse response = ssafyCreditCardClient.createCreditCard(
                userKey, request.cardUniqueNo(), request.withdrawalAccountNo(), request.withdrawalDate());

        SsafyCreateCreditCardResponse.CreditCardRec rec = response.rec();
        String cardNo = rec.cardNo();
        String last4 = cardNo.substring(cardNo.length() - 4);

        Card card = Card.builder()
                .user(user)
                .cardName(request.cardName())
                .cardType(request.cardType())
                .last4Digits(last4)
                .cardNo(cardNo)
                .cvc(rec.cvc())
                .cardUniqueNo(rec.cardUniqueNo())
                .ssafyAccountNo(request.withdrawalAccountNo())
                .withdrawalAccountNo(rec.withdrawalAccountNo())
                .withdrawalDate(rec.withdrawalDate())
                .cardExpiryDate(rec.cardExpiryDate())
                .build();

        cardRepository.save(card);
        return CardResponse.from(card);
    }

    /**
     * 내 카드 목록 조회
     */
    public List<CardResponse> getCards(Long userId) {
        return cardRepository.findByUser_IdAndDeletedFalse(userId).stream()
                .map(CardResponse::from)
                .toList();
    }

    /**
     * 카드 상세 조회
     */
    public CardResponse getCard(Long userId, Long cardId) {
        Card card = getCardWithOwnership(userId, cardId);
        return CardResponse.from(card);
    }

    /**
     * 기본 카드 설정
     */
    @Transactional
    public CardResponse setDefaultCard(Long userId, Long cardId) {
        Card card = getCardWithOwnership(userId, cardId);
        cardRepository.findByUser_IdAndIsDefaultTrueAndDeletedFalse(userId)
                .ifPresent(Card::unmarkDefault);
        card.markDefault();
        return CardResponse.from(card);
    }

    /**
     * 카드 삭제
     */
    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        Card card = getCardWithOwnership(userId, cardId);
        card.softDelete();
    }

    /**
     * 카드 결제 (SSAFY 신용카드 결제 API)
     */
    public CardPaymentResponse payment(Long userId, Long cardId, CardPaymentRequest request) {
        User user = getUser(userId);
        Card card = getCardWithOwnership(userId, cardId);
        String userKey = getUserKey(user);

        SsafyCreditCardTransactionResponse response = ssafyCreditCardClient.createTransaction(
                userKey, card.getCardNo(), card.getCvc(), request.merchantId(), request.paymentBalance());

        SsafyCreditCardTransactionResponse.TransactionRec rec = response.rec();
        return new CardPaymentResponse(
                rec.transactionUniqueNo(),
                card.getId(),
                rec.merchantName(),
                rec.categoryName(),
                rec.paymentBalance(),
                rec.transactionDate(),
                rec.transactionTime()
        );
    }

    /**
     * 카드 결제 취소
     */
    public void cancelPayment(Long userId, Long cardId, CancelPaymentRequest request) {
        User user = getUser(userId);
        Card card = getCardWithOwnership(userId, cardId);
        String userKey = getUserKey(user);

        ssafyCreditCardClient.deleteTransaction(userKey, card.getCardNo(), card.getCvc(), request.transactionUniqueNo());
    }

    /**
     * 카드 결제 내역 조회 (SSAFY API에서 직접 조회)
     */
    public List<CardTransactionResponse> getTransactions(Long userId, Long cardId,
                                                          String startDate, String endDate) {
        User user = getUser(userId);
        Card card = getCardWithOwnership(userId, cardId);
        String userKey = getUserKey(user);

        SsafyCreditCardTransactionListResponse response = ssafyCreditCardClient.getTransactionHistory(
                userKey, card.getCardNo(), card.getCvc(), startDate, endDate);

        if (response.rec() == null) {
            return List.of();
        }

        return response.rec().stream()
                .map(CardTransactionResponse::from)
                .toList();
    }

    /**
     * 청구서 조회
     */
    public List<CardBillingResponse> getBillingStatements(Long userId, Long cardId,
                                                           String startMonth, String endMonth) {
        User user = getUser(userId);
        Card card = getCardWithOwnership(userId, cardId);
        String userKey = getUserKey(user);

        SsafyBillingStatementsResponse response = ssafyCreditCardClient.getBillingStatements(
                userKey, card.getCardNo(), card.getCvc(), startMonth, endMonth);

        if (response.rec() == null) {
            return List.of();
        }

        return response.rec().stream()
                .map(CardBillingResponse::from)
                .toList();
    }

    /**
     * 카드 활성화
     */
    @Transactional
    public CardActivateResponse activateCard(Long userId, Long cardId, CardActivateRequest request) {
        Card card = getCardWithOwnership(userId, cardId);

        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 활성화된 카드입니다.");
        }

        card.activate();

        return new CardActivateResponse(
                card.getId(),
                card.getStatus().name(),
                card.getUpdatedAt()
        );
    }

    /**
     * 카드 용도 변경
     */
    @Transactional
    public CardPurposeResponse setCardPurpose(Long userId, Long cardId, CardPurposeRequest request) {
        Card card = getCardWithOwnership(userId, cardId);
        card.updateDefaultPurpose(request.defaultPurpose());

        return new CardPurposeResponse(
                card.getId(),
                card.getDefaultPurpose(),
                card.getUpdatedAt()
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Card getCardWithOwnership(Long userId, Long cardId) {
        return cardRepository.findByIdAndUser_IdAndDeletedFalse(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
    }

    private String getUserKey(User user) {
        if (user.getSsafyUserKey() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "SSAFY 금융망 사용자 키가 등록되지 않았습니다.");
        }
        return user.getSsafyUserKey();
    }
}
