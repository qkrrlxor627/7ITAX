package com.ssafy.tax7i.banking.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyAccountListResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyBalanceResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyCreateAccountResponse;
import com.ssafy.tax7i.banking.dto.AccountCreateRequest;
import com.ssafy.tax7i.banking.dto.AccountResponse;
import com.ssafy.tax7i.banking.dto.BalanceResponse;
import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.AccountType;
import com.ssafy.tax7i.banking.repository.AccountRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SsafyFinanceClient ssafyFinanceClient;

    @Transactional
    public AccountResponse createAccount(Long userId, AccountCreateRequest request) {
        User user = getUser(userId);
        String userKey = getUserKey(user);

        // FE가 bankCode로 보내지만 SSAFY API는 accountTypeUniqueNo로 사용
        SsafyCreateAccountResponse response = ssafyFinanceClient.createAccount(userKey, request.bankCode());
        SsafyCreateAccountResponse.AccountRec rec = response.rec();
        if (rec == null) {
            throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, "계좌 개설 응답이 비어있습니다.");
        }

        Account account = Account.builder()
                .user(user)
                .accountType(request.accountType())
                .bankCode(rec.bankCode())
                .accountNo(rec.accountNo())
                .accountTypeUniqueNo(request.bankCode())
                .alias(request.alias())
                .build();

        accountRepository.save(account);
        return AccountResponse.from(account, 0L);
    }

    public List<AccountResponse> getAccounts(Long userId, String accountType) {
        User user = getUser(userId);
        String userKey = getUserKey(user);

        // DB에서 계좌 목록 조회 (accountType 필터 옵션)
        List<Account> accounts;
        if (accountType != null && !accountType.isBlank()) {
            AccountType type;
            try {
                type = AccountType.valueOf(accountType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "유효하지 않은 계좌 유형: " + accountType);
            }
            accounts = accountRepository.findByUser_IdAndAccountTypeAndDeletedFalse(userId, type);
        } else {
            accounts = accountRepository.findByUser_IdAndDeletedFalse(userId);
        }

        // SSAFY에서 잔액 일괄 조회
        SsafyAccountListResponse ssafyResponse = ssafyFinanceClient.getAccountList(userKey);
        Map<String, Long> balanceMap = Map.of();
        if (ssafyResponse.rec() != null) {
            balanceMap = ssafyResponse.rec().stream()
                    .collect(Collectors.toMap(
                            SsafyAccountListResponse.AccountRec::accountNo,
                            r -> parseBalance(r.accountBalance()),
                            (a, b) -> a  // 중복 시 첫 번째 값 사용
                    ));
        }

        Map<String, Long> finalBalanceMap = balanceMap;
        return accounts.stream()
                .map(account -> AccountResponse.from(
                        account,
                        finalBalanceMap.getOrDefault(account.getAccountNo(), 0L)
                ))
                .toList();
    }

    public AccountResponse getAccount(Long userId, Long accountId) {
        User user = getUser(userId);
        String userKey = getUserKey(user);
        Account account = getAccountWithOwnership(userId, accountId);

        SsafyBalanceResponse response = ssafyFinanceClient.getBalance(userKey, account.getAccountNo());
        if (response.rec() == null) {
            throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, "계좌 조회 응답이 비어있습니다.");
        }
        long balance = parseBalance(response.rec().accountBalance());

        return AccountResponse.from(account, balance);
    }

    public BalanceResponse getBalance(Long userId, Long accountId) {
        User user = getUser(userId);
        String userKey = getUserKey(user);
        Account account = getAccountWithOwnership(userId, accountId);

        SsafyBalanceResponse response = ssafyFinanceClient.getBalance(userKey, account.getAccountNo());
        if (response.rec() == null) {
            throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, "잔액 조회 응답이 비어있습니다.");
        }
        long balance = parseBalance(response.rec().accountBalance());

        return BalanceResponse.of(accountId, balance);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Account getAccountWithOwnership(Long userId, Long accountId) {
        return accountRepository.findByIdAndUser_IdAndDeletedFalse(accountId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private long parseBalance(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String getUserKey(User user) {
        if (user.getSsafyUserKey() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "SSAFY 금융망 사용자 키가 등록되지 않았습니다.");
        }
        return user.getSsafyUserKey();
    }
}
