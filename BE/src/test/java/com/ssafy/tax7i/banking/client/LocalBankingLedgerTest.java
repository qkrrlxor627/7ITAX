package com.ssafy.tax7i.banking.client;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.banking.client.dto.SsafyTransferResult;
import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.AccountType;
import com.ssafy.tax7i.banking.repository.AccountRepository;
import com.ssafy.tax7i.banking.repository.BankTransactionRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 로컬 뱅킹 원장(SsafyFinanceClient) 동작 검증.
 * 외부 HTTP 대신 자체 DB 원장 위에서 입금/출금/이체가 동작하는지 확인한다.
 *
 * <p>메인 설정은 PostgreSQLDialect를 쓰지만, 비관적 락(PESSIMISTIC_WRITE)이 Postgres에서는
 * {@code FOR NO KEY UPDATE}로 렌더링되어 H2 테스트 DB가 파싱하지 못한다. 이 테스트에 한해
 * H2Dialect로 오버라이드하여 락이 {@code FOR UPDATE}로 렌더링되도록 한다(운영 Postgres에서는 정상).
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(SsafyFinanceClient.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class LocalBankingLedgerTest {

    @BeforeAll
    static void setup() {
        // JPA/Hibernate가 Spring보다 먼저 AesEncryptor를 초기화할 수 있으므로 AES 키를 미리 설정
        System.setProperty("encryption.aes-key", "dGVzdC1hZXMtZW5jcnlwdGlvbi1rZXktMzItYnl0ZXM=");
    }

    @Autowired
    private SsafyFinanceClient financeClient;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Autowired
    private jakarta.persistence.EntityManager em;

    private Account newAccount(String userKey, String accountNo) {
        User user = User.builder()
                .ci("ci-" + accountNo)
                .di("di-" + accountNo)
                .name("테스터")
                .ssafyUserKey(userKey)
                .build();
        em.persist(user);

        Account account = Account.builder()
                .user(user)
                .accountType(AccountType.BUSINESS)
                .bankCode("999")
                .accountNo(accountNo)
                .accountTypeUniqueNo("001")
                .alias("테스트계좌")
                .build();
        return accountRepository.saveAndFlush(account);
    }

    @Test
    @DisplayName("입금하면 잔액이 증가한다")
    void deposit_increasesBalance() {
        Account account = newAccount("LOCAL_dep", "1111111111111111");
        long before = account.getBalance();

        financeClient.deposit("LOCAL_dep", account.getAccountNoKey(), 10_000L, "입금테스트");

        Account reloaded = accountRepository.findByAccountNoKey("1111111111111111").orElseThrow();
        assertThat(reloaded.getBalance()).isEqualTo(before + 10_000L);
        assertThat(bankTransactionRepository.findByAccountNoKeyOrderByTransactedAtDesc("1111111111111111")).hasSize(1);
    }

    @Test
    @DisplayName("출금하면 잔액이 감소한다")
    void withdraw_decreasesBalance() {
        Account account = newAccount("LOCAL_wd", "2222222222222222");
        long before = account.getBalance();

        financeClient.withdraw("LOCAL_wd", account.getAccountNoKey(), 30_000L, "출금테스트");

        Account reloaded = accountRepository.findByAccountNoKey("2222222222222222").orElseThrow();
        assertThat(reloaded.getBalance()).isEqualTo(before - 30_000L);
    }

    @Test
    @DisplayName("잔액을 초과하여 출금하면 INSUFFICIENT_BALANCE 예외가 발생한다")
    void withdraw_overBalance_throws() {
        Account account = newAccount("LOCAL_over", "3333333333333333");
        long over = account.getBalance() + 1L;

        assertThatThrownBy(() -> financeClient.withdraw("LOCAL_over", account.getAccountNoKey(), over, "초과출금"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("이체하면 두 계좌 잔액이 원자적으로 이동하고 거래가 2건 기록된다")
    void transfer_movesFundsAtomically() {
        Account from = newAccount("LOCAL_from", "4444444444444444");
        Account to = newAccount("LOCAL_to", "5555555555555555");
        long fromBefore = from.getBalance();
        long toBefore = to.getBalance();

        SsafyTransferResult result = financeClient.transfer(
                "LOCAL_from", from.getAccountNoKey(),
                "LOCAL_to", to.getAccountNoKey(),
                100_000L, "이체출금", "이체입금");

        assertThat(result.withdrawResponse().header().isSuccess()).isTrue();
        assertThat(result.depositResponse().header().isSuccess()).isTrue();

        Account fromReloaded = accountRepository.findByAccountNoKey("4444444444444444").orElseThrow();
        Account toReloaded = accountRepository.findByAccountNoKey("5555555555555555").orElseThrow();
        assertThat(fromReloaded.getBalance()).isEqualTo(fromBefore - 100_000L);
        assertThat(toReloaded.getBalance()).isEqualTo(toBefore + 100_000L);

        assertThat(bankTransactionRepository.findByAccountNoKeyOrderByTransactedAtDesc("4444444444444444")).hasSize(1);
        assertThat(bankTransactionRepository.findByAccountNoKeyOrderByTransactedAtDesc("5555555555555555")).hasSize(1);
    }
}
