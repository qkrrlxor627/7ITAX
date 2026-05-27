package com.ssafy.tax7i.banking.client;

import com.ssafy.tax7i.banking.client.dto.*;
import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.BankTransaction;
import com.ssafy.tax7i.banking.entity.BankTransactionType;
import com.ssafy.tax7i.banking.repository.AccountRepository;
import com.ssafy.tax7i.banking.repository.BankTransactionRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import static com.ssafy.tax7i.banking.client.LocalLedgerSupport.*;

/**
 * 로컬 뱅킹 시뮬레이션 클라이언트.
 *
 * <p>기존에는 외부 SSAFY 금융망(HTTP/RestTemplate)을 호출했으나,
 * 이제 자체 구현 DB 원장(ledger) 위에서 동작한다.
 * 공개 메서드 시그니처와 반환 타입(Ssafy*Response)은 그대로 유지하여
 * 소비 서비스(AccountService, TransferService, TaxPaymentService, AuthService)는 변경하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SsafyFinanceClient {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AccountRepository accountRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public SsafyCreateAccountResponse createAccount(String userKey, String accountTypeUniqueNo) {
        String accountNo = generateUniqueAccountNo();
        String txnId = newTransactionUniqueNo();
        // 계좌 영속화는 AccountService가 담당한다. (빌더가 accountNo→accountNoKey 복사 + 데모 잔액 설정)
        return new SsafyCreateAccountResponse(
                successHeader("createDemandDepositAccount", txnId),
                new SsafyCreateAccountResponse.AccountRec(BANK_CODE, accountNo)
        );
    }

    public SsafyAccountListResponse getAccountList(String userKey) {
        List<Account> accounts = accountRepository.findByUser_SsafyUserKeyAndDeletedFalse(userKey);
        List<SsafyAccountListResponse.AccountRec> recs = accounts.stream()
                .map(a -> new SsafyAccountListResponse.AccountRec(
                        a.getBankCode(),
                        a.getAccountNoKey(),
                        a.getAlias() != null && !a.getAlias().isBlank() ? a.getAlias() : "입출금",
                        String.valueOf(a.getBalance())
                ))
                .toList();
        return new SsafyAccountListResponse(successHeader("inquireDemandDepositAccountList", newTransactionUniqueNo()), recs);
    }

    public SsafyBalanceResponse getBalance(String userKey, String accountNo) {
        Account account = accountRepository.findByAccountNoKey(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        return new SsafyBalanceResponse(
                successHeader("inquireDemandDepositAccountBalance", newTransactionUniqueNo()),
                new SsafyBalanceResponse.BalanceRec(
                        account.getBankCode(),
                        account.getAccountNoKey(),
                        String.valueOf(account.getBalance()),
                        today()
                )
        );
    }

    @Transactional
    public SsafyDepositResponse deposit(String userKey, String accountNo, long amount, String summary) {
        Account account = accountRepository.findByAccountNoKeyForUpdate(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.deposit(amount);
        String txnId = recordTransaction(account, BankTransactionType.DEPOSIT, amount, summary);
        return new SsafyDepositResponse(
                successHeader("updateDemandDepositAccountDeposit", txnId),
                new SsafyDepositResponse.DepositRec(
                        txnId, account.getAccountNoKey(), today(),
                        String.valueOf(amount), String.valueOf(account.getBalance())
                )
        );
    }

    @Transactional
    public SsafyWithdrawResponse withdraw(String userKey, String accountNo, long amount, String summary) {
        Account account = accountRepository.findByAccountNoKeyForUpdate(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.withdraw(amount); // 잔액 부족 시 INSUFFICIENT_BALANCE 전파
        String txnId = recordTransaction(account, BankTransactionType.WITHDRAWAL, amount, summary);
        return new SsafyWithdrawResponse(
                successHeader("updateDemandDepositAccountWithdrawal", txnId),
                new SsafyWithdrawResponse.WithdrawRec(
                        txnId, account.getAccountNoKey(), today(),
                        String.valueOf(amount), String.valueOf(account.getBalance())
                )
        );
    }

    @Transactional
    public SsafyTransferResult transfer(String senderUserKey, String fromAccountNo,
                                        String receiverUserKey, String toAccountNo,
                                        long amount, String withdrawSummary, String depositSummary) {
        // 데드락 방지: 두 계좌의 id를 먼저 확인해 항상 id 오름차순으로 비관적 락을 획득한다.
        Account fromPreview = accountRepository.findByAccountNoKey(fromAccountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        Account toPreview = accountRepository.findByAccountNoKey(toAccountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        List<String> lockOrder = java.util.stream.Stream.of(fromPreview, toPreview)
                .sorted(Comparator.comparing(Account::getId))
                .map(Account::getAccountNoKey)
                .toList();
        for (String key : lockOrder) {
            accountRepository.findByAccountNoKeyForUpdate(key)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        }

        // 잠금 완료 후 송신/수신 계좌를 명시적으로 다시 로드(영속성 컨텍스트 내 동일 인스턴스).
        Account sender = accountRepository.findByAccountNoKeyForUpdate(fromAccountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        Account receiver = accountRepository.findByAccountNoKeyForUpdate(toAccountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        sender.withdraw(amount); // 잔액 부족 시 INSUFFICIENT_BALANCE 전파 → 트랜잭션 롤백
        String withdrawTxnId = recordTransaction(sender, BankTransactionType.WITHDRAWAL, amount, withdrawSummary);

        receiver.deposit(amount);
        String depositTxnId = recordTransaction(receiver, BankTransactionType.DEPOSIT, amount, depositSummary);

        SsafyWithdrawResponse withdrawResp = new SsafyWithdrawResponse(
                successHeader("updateDemandDepositAccountWithdrawal", withdrawTxnId),
                new SsafyWithdrawResponse.WithdrawRec(
                        withdrawTxnId, sender.getAccountNoKey(), today(),
                        String.valueOf(amount), String.valueOf(sender.getBalance())
                )
        );
        SsafyDepositResponse depositResp = new SsafyDepositResponse(
                successHeader("updateDemandDepositAccountDeposit", depositTxnId),
                new SsafyDepositResponse.DepositRec(
                        depositTxnId, receiver.getAccountNoKey(), today(),
                        String.valueOf(amount), String.valueOf(receiver.getBalance())
                )
        );
        return new SsafyTransferResult(withdrawResp, depositResp);
    }

    public SsafyTransactionHistoryResponse getTransactionHistory(String userKey, String accountNo,
                                                                 String startDate, String endDate) {
        LocalDateTime start = parseDate(startDate, true).atStartOfDay();
        LocalDateTime end = parseDate(endDate, false).atTime(java.time.LocalTime.MAX);

        List<BankTransaction> txns = bankTransactionRepository
                .findByAccountNoKeyAndTransactedAtBetweenOrderByTransactedAtDesc(accountNo, start, end);

        List<SsafyTransactionHistoryResponse.TransactionRecord> recs = txns.stream()
                .map(tx -> {
                    boolean isDeposit = tx.getTransactionType() == BankTransactionType.DEPOSIT;
                    return new SsafyTransactionHistoryResponse.TransactionRecord(
                            tx.getTransactionUniqueNo(),
                            tx.getTransactedAt().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                            tx.getTransactedAt().format(DateTimeFormatter.ofPattern("HHmmss")),
                            isDeposit ? "1" : "2",
                            isDeposit ? "입금" : "출금",
                            tx.getAccountNoKey(),
                            String.valueOf(tx.getAmount()),
                            String.valueOf(tx.getBalanceAfter()),
                            tx.getSummary(),
                            tx.getMemo()
                    );
                })
                .toList();
        return new SsafyTransactionHistoryResponse(
                successHeader("inquireTransactionHistoryList", newTransactionUniqueNo()), recs);
    }

    // ───────────── 멤버 (로컬 결정적 키) ─────────────

    public String searchMember(String userId) {
        return localUserKey(userId);
    }

    public String registerMember(String userId) {
        return localUserKey(userId);
    }

    public String getOrRegisterMember(String userId) {
        return localUserKey(userId);
    }

    private String localUserKey(String userId) {
        return "LOCAL_" + userId;
    }

    // ───────────── 내부 헬퍼 ─────────────

    private String recordTransaction(Account account, BankTransactionType type, long amount, String summary) {
        String txnId = newTransactionUniqueNo();
        bankTransactionRepository.save(BankTransaction.builder()
                .accountNoKey(account.getAccountNoKey())
                .transactionUniqueNo(txnId)
                .transactionType(type)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .summary(summary)
                .transactedAt(LocalDateTime.now())
                .build());
        return txnId;
    }

    private String generateUniqueAccountNo() {
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder(16);
            for (int j = 0; j < 16; j++) {
                sb.append(RANDOM.nextInt(10));
            }
            String candidate = sb.toString();
            if (accountRepository.findByAccountNoKey(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, "계좌번호 생성에 실패했습니다.");
    }

    private LocalDate parseDate(String yyyyMMdd, boolean isStart) {
        if (yyyyMMdd == null || yyyyMMdd.isBlank()) {
            return isStart ? LocalDate.now().minusYears(1) : LocalDate.now();
        }
        return LocalDate.parse(yyyyMMdd, DATE_FMT);
    }
}
