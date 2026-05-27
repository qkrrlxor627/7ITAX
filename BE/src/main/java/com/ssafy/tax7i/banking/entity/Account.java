package com.ssafy.tax7i.banking.entity;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.global.crypto.AesEncryptor;
import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_account_user_id", columnList = "user_id"),
        @Index(name = "idx_account_no_key", columnList = "account_no_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseTimeEntity {

    /**
     * 로컬 뱅킹 시뮬레이션 편의를 위한 신규 계좌 초기 잔액.
     * 실제 금융망 연동이 아닌 자체 구현 원장(ledger)이므로,
     * 모든 흐름(이체/결제/납부)이 바로 실행 가능하도록 데모용 시드 잔액을 부여한다.
     */
    private static final long DEMO_INITIAL_BALANCE = 5_000_000L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Column(nullable = false)
    private String bankCode;

    @Convert(converter = AesEncryptor.class)
    @Column(nullable = false, length = 512)
    private String accountNo;

    /**
     * 평문 조회 키. accountNo는 AES/GCM(랜덤 IV)으로 암호화되어 매번 암호문이 달라
     * 컬럼으로 조회할 수 없으므로, 평문 인덱스 키를 별도로 보관해 조회에 사용한다.
     */
    @Column(name = "account_no_key", unique = true)
    private String accountNoKey;

    @Column(nullable = false)
    private long balance;

    @Version
    private Long version;

    @Column(nullable = false)
    private String accountTypeUniqueNo;

    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false)
    private boolean deleted = false;

    @Builder
    public Account(User user, AccountType accountType, String bankCode,
                   String accountNo, String accountTypeUniqueNo, String alias) {
        this.user = user;
        this.accountType = accountType;
        this.bankCode = bankCode;
        this.accountNo = accountNo;
        // 평문 조회 키를 빌더 생성 시점에 자동 복사 (AccountService 변경 불필요)
        this.accountNoKey = accountNo;
        this.accountTypeUniqueNo = accountTypeUniqueNo;
        this.alias = alias;
        this.status = AccountStatus.ACTIVE;
        this.deleted = false;
        // 로컬 시뮬레이션 편의용 데모 시드 잔액
        this.balance = DEMO_INITIAL_BALANCE;
    }

    public void close() {
        this.status = AccountStatus.CLOSED;
    }

    public void softDelete() {
        this.deleted = true;
    }

    public void deposit(long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "입금 금액은 0보다 커야 합니다.");
        }
        this.balance += amount;
    }

    public void withdraw(long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "출금 금액은 0보다 커야 합니다.");
        }
        if (amount > this.balance) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance -= amount;
    }
}
