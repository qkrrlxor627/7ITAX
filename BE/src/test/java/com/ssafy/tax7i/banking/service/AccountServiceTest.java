package com.ssafy.tax7i.banking.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyAccountListResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyBalanceResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyCreateAccountResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyResponseHeader;
import com.ssafy.tax7i.banking.dto.AccountCreateRequest;
import com.ssafy.tax7i.banking.dto.AccountResponse;
import com.ssafy.tax7i.banking.dto.BalanceResponse;
import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.AccountType;
import com.ssafy.tax7i.banking.repository.AccountRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
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

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private SsafyFinanceClient ssafyFinanceClient;

    @InjectMocks
    private AccountService accountService;

    // ─────────── createAccount ───────────

    @Test
    void createAccount_성공() {
        User user = createUser(1L, "test-user-key");
        Account account = createAccount(1L, user, AccountType.BUSINESS, "110-123-456789");

        SsafyCreateAccountResponse ssafyResponse = new SsafyCreateAccountResponse(
                successHeader(),
                new SsafyCreateAccountResponse.AccountRec("088", "110-123-456789")
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(ssafyFinanceClient.createAccount("test-user-key", "088-1-XXXXXXXXX")).willReturn(ssafyResponse);
        given(accountRepository.save(any(Account.class))).willReturn(account);

        AccountCreateRequest request = new AccountCreateRequest(AccountType.BUSINESS, "088-1-XXXXXXXXX", "사업용 계좌");

        AccountResponse response = accountService.createAccount(1L, request);

        assertThat(response.accountType()).isEqualTo("BUSINESS");
        assertThat(response.bankCode()).isEqualTo("088");
        assertThat(response.balance()).isEqualTo(0L);
    }

    @Test
    void createAccount_SSAFY응답없음_예외() {
        User user = createUser(1L, "test-user-key");

        SsafyCreateAccountResponse ssafyResponse = new SsafyCreateAccountResponse(successHeader(), null);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(ssafyFinanceClient.createAccount("test-user-key", "088-1-XXXXXXXXX")).willReturn(ssafyResponse);

        AccountCreateRequest request = new AccountCreateRequest(AccountType.BUSINESS, "088-1-XXXXXXXXX", null);

        assertThatThrownBy(() -> accountService.createAccount(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BANK_SERVICE_UNAVAILABLE));
    }

    // ─────────── getAccounts ───────────

    @Test
    void getAccounts_전체조회() {
        User user = createUser(1L, "test-user-key");
        Account account1 = createAccount(1L, user, AccountType.PERSONAL, "110-111-111111");
        Account account2 = createAccount(2L, user, AccountType.BUSINESS, "110-222-222222");

        SsafyAccountListResponse ssafyResponse = new SsafyAccountListResponse(
                successHeader(),
                List.of(
                        new SsafyAccountListResponse.AccountRec("088", "110-111-111111", "입출금통장", "50000"),
                        new SsafyAccountListResponse.AccountRec("088", "110-222-222222", "사업용통장", "100000")
                )
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(accountRepository.findByUser_IdAndDeletedFalse(1L)).willReturn(List.of(account1, account2));
        given(ssafyFinanceClient.getAccountList("test-user-key")).willReturn(ssafyResponse);

        List<AccountResponse> responses = accountService.getAccounts(1L, null);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).balance()).isEqualTo(50000L);
        assertThat(responses.get(1).balance()).isEqualTo(100000L);
    }

    @Test
    void getAccounts_타입필터() {
        User user = createUser(1L, "test-user-key");
        Account account = createAccount(1L, user, AccountType.PERSONAL, "110-111-111111");

        SsafyAccountListResponse ssafyResponse = new SsafyAccountListResponse(
                successHeader(),
                List.of(new SsafyAccountListResponse.AccountRec("088", "110-111-111111", "입출금통장", "30000"))
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(accountRepository.findByUser_IdAndAccountTypeAndDeletedFalse(1L, AccountType.PERSONAL))
                .willReturn(List.of(account));
        given(ssafyFinanceClient.getAccountList("test-user-key")).willReturn(ssafyResponse);

        List<AccountResponse> responses = accountService.getAccounts(1L, "PERSONAL");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).accountType()).isEqualTo("PERSONAL");
        assertThat(responses.get(0).balance()).isEqualTo(30000L);
    }

    @Test
    void getAccounts_잘못된타입_예외() {
        User user = createUser(1L, "test-user-key");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> accountService.getAccounts(1L, "INVALID_TYPE"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_ARGUMENT));
    }

    // ─────────── getAccount ───────────

    @Test
    void getAccount_성공() {
        User user = createUser(1L, "test-user-key");
        Account account = createAccount(1L, user, AccountType.BUSINESS, "110-123-456789");

        SsafyBalanceResponse ssafyResponse = new SsafyBalanceResponse(
                successHeader(),
                new SsafyBalanceResponse.BalanceRec("088", "110-123-456789", "75000", "20260325")
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(accountRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(account));
        given(ssafyFinanceClient.getBalance("test-user-key", "110-123-456789")).willReturn(ssafyResponse);

        AccountResponse response = accountService.getAccount(1L, 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.balance()).isEqualTo(75000L);
        assertThat(response.accountType()).isEqualTo("BUSINESS");
    }

    @Test
    void getAccount_소유권불일치_예외() {
        User user = createUser(1L, "test-user-key");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(accountRepository.findByIdAndUser_IdAndDeletedFalse(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    // ─────────── getBalance ───────────

    @Test
    void getBalance_성공() {
        User user = createUser(1L, "test-user-key");
        Account account = createAccount(1L, user, AccountType.PERSONAL, "110-111-111111");

        SsafyBalanceResponse ssafyResponse = new SsafyBalanceResponse(
                successHeader(),
                new SsafyBalanceResponse.BalanceRec("088", "110-111-111111", "123456", "20260325")
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(accountRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(account));
        given(ssafyFinanceClient.getBalance("test-user-key", "110-111-111111")).willReturn(ssafyResponse);

        BalanceResponse response = accountService.getBalance(1L, 1L);

        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.balance()).isEqualTo(123456L);
        assertThat(response.availableBalance()).isEqualTo(123456L);
        assertThat(response.pendingAmount()).isEqualTo(0L);
    }

    @Test
    void getBalance_SSAFY응답없음_예외() {
        User user = createUser(1L, "test-user-key");
        Account account = createAccount(1L, user, AccountType.PERSONAL, "110-111-111111");

        SsafyBalanceResponse ssafyResponse = new SsafyBalanceResponse(successHeader(), null);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(accountRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(account));
        given(ssafyFinanceClient.getBalance("test-user-key", "110-111-111111")).willReturn(ssafyResponse);

        assertThatThrownBy(() -> accountService.getBalance(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BANK_SERVICE_UNAVAILABLE));
    }

    // ─────────── helpers ───────────

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

    private Account createAccount(Long id, User user, AccountType accountType, String accountNo) {
        Account account = Account.builder()
                .user(user)
                .accountType(accountType)
                .bankCode("088")
                .accountNo(accountNo)
                .accountTypeUniqueNo("088-1-XXXXXXXXX")
                .alias(null)
                .build();
        setField(account, "id", id);
        return account;
    }

    private SsafyResponseHeader successHeader() {
        return new SsafyResponseHeader(
                "H0000", "정상처리 되었습니다.", "test", "20260326", "120000",
                "00100", "test-api-key", "test", "uniqueNo");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    var field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Field not found: " + fieldName);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
