package com.ssafy.tax7i.banking.client;

import com.ssafy.tax7i.banking.client.dto.*;
import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.BankTransaction;
import com.ssafy.tax7i.banking.entity.BankTransactionType;
import com.ssafy.tax7i.banking.repository.AccountRepository;
import com.ssafy.tax7i.banking.repository.BankTransactionRepository;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardLedgerTransaction;
import com.ssafy.tax7i.card.repository.CardLedgerTransactionRepository;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.ssafy.tax7i.banking.client.LocalLedgerSupport.*;

/**
 * 로컬 카드 시뮬레이션 클라이언트.
 *
 * <p>외부 SSAFY 신용카드 API(HTTP) 대신 자체 DB 원장 위에서 동작한다.
 * 카드 결제는 직불(debit) 모델로 처리하여, 결제 즉시 카드의 출금계좌(withdrawalAccountNo)에서
 * 로컬 원장을 통해 차감하고 카드 결제 내역을 별도로 기록한다.
 *
 * <p>공개 메서드 시그니처/반환 타입(Ssafy*Response)은 그대로 유지하여
 * 소비 서비스(CardService, PaymentService)는 변경하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SsafyCreditCardClient {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final CardLedgerTransactionRepository cardLedgerTransactionRepository;

    // ───────────── 카드 상품 / 가맹점 카탈로그 (인-코드) ─────────────

    private record CardProduct(String cardUniqueNo, String issuerCode, String issuerName, String cardName,
                               String baselinePerformance, String maxBenefitLimit, String description) {}

    private static final List<CardProduct> CARD_CATALOG = List.of(
            new CardProduct("LOCAL-CARD-001", "999", "7I은행", "7I 비즈니스 카드",
                    "300000", "30000", "사업자 지출에 최적화된 로컬 시뮬레이션 카드"),
            new CardProduct("LOCAL-CARD-002", "999", "7I은행", "7I 라이프 카드",
                    "200000", "20000", "생활 전반 혜택 로컬 시뮬레이션 카드"),
            new CardProduct("LOCAL-CARD-003", "999", "7I은행", "7I 트래블 카드",
                    "500000", "50000", "여행/교통 혜택 로컬 시뮬레이션 카드")
    );

    private record MerchantSeed(long id, String name, String categoryId, String categoryName) {}

    private static final List<MerchantSeed> MERCHANT_CATALOG = List.of(
            new MerchantSeed(1L, "스타벅스", "5814", "음식점"),
            new MerchantSeed(2L, "이마트", "5411", "마트/슈퍼"),
            new MerchantSeed(3L, "GS25", "5411", "편의점"),
            new MerchantSeed(4L, "쿠팡", "5999", "온라인쇼핑"),
            new MerchantSeed(5L, "KTX", "4112", "교통"),
            new MerchantSeed(6L, "주유소", "5541", "주유")
    );

    public SsafyCreditCardProductListResponse getCreditCardProducts(String userKey) {
        List<SsafyCreditCardProductListResponse.CreditCardProductRec> recs = CARD_CATALOG.stream()
                .map(p -> new SsafyCreditCardProductListResponse.CreditCardProductRec(
                        p.cardUniqueNo(), p.issuerCode(), p.issuerName(), p.cardName(),
                        p.baselinePerformance(), p.maxBenefitLimit(), p.description()))
                .toList();
        return new SsafyCreditCardProductListResponse(successHeader("inquireCreditCardList", newTransactionUniqueNo()), recs);
    }

    public SsafyMerchantListResponse getMerchantList(String userKey) {
        List<SsafyMerchantListResponse.MerchantRec> recs = MERCHANT_CATALOG.stream()
                .map(m -> new SsafyMerchantListResponse.MerchantRec(m.id(), m.name(), m.categoryId(), m.categoryName()))
                .toList();
        return new SsafyMerchantListResponse(successHeader("inquireMerchantList", newTransactionUniqueNo()), recs);
    }

    public SsafyCreateCreditCardResponse createCreditCard(String userKey, String cardUniqueNo,
                                                          String withdrawalAccountNo, String withdrawalDate) {
        CardProduct product = CARD_CATALOG.stream()
                .filter(p -> p.cardUniqueNo().equals(cardUniqueNo))
                .findFirst()
                .orElse(CARD_CATALOG.get(0));

        String cardNo = generateCardNo();
        String cvc = String.format("%03d", RANDOM.nextInt(1000));
        String expiry = YearMonth.now().plusYears(5).format(DateTimeFormatter.ofPattern("MMyy"));

        // 카드 영속화는 CardService가 담당한다. (빌더가 cardNo→cardNoKey 복사)
        return new SsafyCreateCreditCardResponse(
                successHeader("createCreditCard", newTransactionUniqueNo()),
                new SsafyCreateCreditCardResponse.CreditCardRec(
                        cardNo, cvc, product.cardUniqueNo(), product.issuerCode(), product.issuerName(),
                        product.cardName(), product.baselinePerformance(), product.maxBenefitLimit(),
                        product.description(), expiry, withdrawalAccountNo, withdrawalDate
                )
        );
    }

    public SsafyCreditCardListResponse getMyCards(String userKey) {
        List<Card> cards = cardRepository.findByUser_SsafyUserKeyAndDeletedFalse(userKey);
        List<SsafyCreditCardListResponse.CreditCardRec> recs = cards.stream()
                .map(c -> {
                    CardProduct p = CARD_CATALOG.stream()
                            .filter(cp -> cp.cardUniqueNo().equals(c.getCardUniqueNo()))
                            .findFirst()
                            .orElse(CARD_CATALOG.get(0));
                    return new SsafyCreditCardListResponse.CreditCardRec(
                            c.getCardNoKey(), c.getCvc(), c.getCardUniqueNo(), p.issuerCode(), p.issuerName(),
                            c.getCardName(), p.baselinePerformance(), p.maxBenefitLimit(), p.description(),
                            c.getCardExpiryDate(), c.getWithdrawalAccountNo(), c.getWithdrawalDate());
                })
                .toList();
        return new SsafyCreditCardListResponse(successHeader("inquireSignUpCreditCardList", newTransactionUniqueNo()), recs);
    }

    @Transactional
    public SsafyCreditCardTransactionResponse createTransaction(String userKey, String cardNo, String cvc,
                                                                Long merchantId, Long paymentBalance) {
        Card card = cardRepository.findByCardNoKey(cardNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        long amount = paymentBalance == null ? 0L : paymentBalance;

        // 직불(debit) 모델: 결제 즉시 출금계좌에서 로컬 원장으로 차감.
        debitWithdrawalAccount(card, amount, "카드결제");

        MerchantSeed merchant = resolveMerchant(merchantId);
        String txnId = newTransactionUniqueNo();
        LocalDateTime now = LocalDateTime.now();

        cardLedgerTransactionRepository.save(CardLedgerTransaction.builder()
                .cardNoKey(card.getCardNoKey())
                .transactionUniqueNo(txnId)
                .merchantId(merchant != null ? merchant.id() : merchantId)
                .merchantName(merchant != null ? merchant.name() : null)
                .categoryId(merchant != null ? merchant.categoryId() : null)
                .categoryName(merchant != null ? merchant.categoryName() : null)
                .paymentBalance(amount)
                .status(CardLedgerTransaction.STATUS_SUCCESS)
                .transactedAt(now)
                .build());

        return new SsafyCreditCardTransactionResponse(
                successHeader("createCreditCardTransaction", txnId),
                new SsafyCreditCardTransactionResponse.TransactionRec(
                        Long.parseLong(txnId),
                        card.getCardNoKey(),
                        merchant != null ? merchant.id() : merchantId,
                        merchant != null ? merchant.name() : null,
                        merchant != null ? merchant.categoryId() : null,
                        merchant != null ? merchant.categoryName() : null,
                        amount,
                        now.format(DATE_FMT),
                        now.format(DateTimeFormatter.ofPattern("HHmmss")),
                        CardLedgerTransaction.STATUS_SUCCESS
                )
        );
    }

    public SsafyCreditCardTransactionListResponse getTransactionHistory(String userKey, String cardNo,
                                                                        String cvc, String startDate, String endDate) {
        Card card = cardRepository.findByCardNoKey(cardNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        LocalDateTime start = parseDate(startDate, true).atStartOfDay();
        LocalDateTime end = parseDate(endDate, false).atTime(LocalTime.MAX);

        List<SsafyCreditCardTransactionListResponse.TransactionRec> recs = cardLedgerTransactionRepository
                .findByCardNoKeyAndTransactedAtBetweenOrderByTransactedAtDesc(card.getCardNoKey(), start, end)
                .stream()
                .map(t -> new SsafyCreditCardTransactionListResponse.TransactionRec(
                        Long.parseLong(t.getTransactionUniqueNo()),
                        t.getTransactedAt().format(DATE_FMT),
                        t.getTransactedAt().format(DateTimeFormatter.ofPattern("HHmmss")),
                        card.getCardNoKey(),
                        t.getMerchantId(),
                        t.getMerchantName(),
                        t.getCategoryId(),
                        t.getCategoryName(),
                        t.getPaymentBalance(),
                        t.getStatus()))
                .toList();
        return new SsafyCreditCardTransactionListResponse(
                successHeader("inquireCreditCardTransactionList", newTransactionUniqueNo()), recs);
    }

    @Transactional
    public SsafyDeleteCreditCardTransactionResponse deleteTransaction(String userKey, String cardNo,
                                                                      String cvc, Long transactionUniqueNo) {
        CardLedgerTransaction txn = cardLedgerTransactionRepository
                .findByTransactionUniqueNo(String.valueOf(transactionUniqueNo))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드 결제 내역을 찾을 수 없습니다."));

        if (!CardLedgerTransaction.STATUS_CANCELLED.equals(txn.getStatus())) {
            txn.cancel();
            // 직불 모델: 취소 시 출금계좌로 환불(입금).
            cardRepository.findByCardNoKey(txn.getCardNoKey())
                    .ifPresent(card -> refundWithdrawalAccount(card, txn.getPaymentBalance(), "카드결제취소"));
        }

        return new SsafyDeleteCreditCardTransactionResponse(
                successHeader("deleteTransaction", newTransactionUniqueNo()),
                new SsafyDeleteCreditCardTransactionResponse.DeletedTransactionRec(
                        transactionUniqueNo, CardLedgerTransaction.STATUS_CANCELLED)
        );
    }

    public SsafyBillingStatementsResponse getBillingStatements(String userKey, String cardNo,
                                                               String cvc, String startMonth, String endMonth) {
        Card card = cardRepository.findByCardNoKey(cardNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        YearMonth start = parseMonth(startMonth, true);
        YearMonth end = parseMonth(endMonth, false);

        LocalDateTime rangeStart = start.atDay(1).atStartOfDay();
        LocalDateTime rangeEnd = end.atEndOfMonth().atTime(LocalTime.MAX);

        // 월별 결제 합계 집계 (취소 건 제외).
        Map<String, Long> monthlyTotals = new TreeMap<>();
        cardLedgerTransactionRepository
                .findByCardNoKeyAndTransactedAtBetweenOrderByTransactedAtDesc(card.getCardNoKey(), rangeStart, rangeEnd)
                .stream()
                .filter(t -> CardLedgerTransaction.STATUS_SUCCESS.equals(t.getStatus()))
                .forEach(t -> monthlyTotals.merge(t.getTransactedAt().format(MONTH_FMT), t.getPaymentBalance(), Long::sum));

        // 조회 범위 내 모든 월에 대해 항목 생성 (결제 없는 월은 0원).
        Map<String, Long> filled = new LinkedHashMap<>();
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            String key = ym.format(MONTH_FMT);
            filled.put(key, monthlyTotals.getOrDefault(key, 0L));
        }

        List<SsafyBillingStatementsResponse.BillingRec> recs = filled.entrySet().stream()
                .map(e -> {
                    YearMonth ym = YearMonth.parse(e.getKey(), MONTH_FMT);
                    long amount = e.getValue();
                    String billingDate = ym.atEndOfMonth().format(DATE_FMT);
                    String dueDate = ym.plusMonths(1).atDay(Math.min(14, ym.plusMonths(1).lengthOfMonth())).format(DATE_FMT);
                    return new SsafyBillingStatementsResponse.BillingRec(
                            e.getKey(), amount, 0L, amount, billingDate, dueDate, "UNPAID");
                })
                .toList();
        return new SsafyBillingStatementsResponse(successHeader("inquireBillingStatements", newTransactionUniqueNo()), recs);
    }

    // ───────────── 내부 헬퍼 ─────────────

    private void debitWithdrawalAccount(Card card, long amount, String summary) {
        if (amount <= 0) {
            return;
        }
        Account account = accountRepository.findByAccountNoKeyForUpdate(card.getWithdrawalAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.withdraw(amount); // 잔액 부족 시 INSUFFICIENT_BALANCE 전파
        recordBankTransaction(account, BankTransactionType.WITHDRAWAL, amount, summary);
    }

    private void refundWithdrawalAccount(Card card, long amount, String summary) {
        if (amount <= 0) {
            return;
        }
        accountRepository.findByAccountNoKeyForUpdate(card.getWithdrawalAccountNo())
                .ifPresent(account -> {
                    account.deposit(amount);
                    recordBankTransaction(account, BankTransactionType.DEPOSIT, amount, summary);
                });
    }

    private void recordBankTransaction(Account account, BankTransactionType type, long amount, String summary) {
        bankTransactionRepository.save(BankTransaction.builder()
                .accountNoKey(account.getAccountNoKey())
                .transactionUniqueNo(newTransactionUniqueNo())
                .transactionType(type)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .summary(summary)
                .transactedAt(LocalDateTime.now())
                .build());
    }

    private MerchantSeed resolveMerchant(Long merchantId) {
        if (merchantId == null) {
            return null;
        }
        return MERCHANT_CATALOG.stream()
                .filter(m -> m.id() == merchantId)
                .findFirst()
                .orElse(null);
    }

    private String generateCardNo() {
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder(16);
            for (int j = 0; j < 16; j++) {
                sb.append(RANDOM.nextInt(10));
            }
            String candidate = sb.toString();
            if (cardRepository.findByCardNoKey(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, "카드번호 생성에 실패했습니다.");
    }

    private LocalDate parseDate(String yyyyMMdd, boolean isStart) {
        if (yyyyMMdd == null || yyyyMMdd.isBlank()) {
            return isStart ? LocalDate.now().minusYears(1) : LocalDate.now();
        }
        return LocalDate.parse(yyyyMMdd, DATE_FMT);
    }

    private YearMonth parseMonth(String yyyyMM, boolean isStart) {
        if (yyyyMM == null || yyyyMM.isBlank()) {
            return isStart ? YearMonth.now().minusMonths(11) : YearMonth.now();
        }
        return YearMonth.parse(yyyyMM, MONTH_FMT);
    }
}
