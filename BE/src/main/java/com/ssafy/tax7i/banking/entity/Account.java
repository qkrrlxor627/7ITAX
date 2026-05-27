package com.ssafy.tax7i.banking.entity;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.global.crypto.AesEncryptor;
import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_account_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseTimeEntity {

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
        this.accountTypeUniqueNo = accountTypeUniqueNo;
        this.alias = alias;
        this.status = AccountStatus.ACTIVE;
        this.deleted = false;
    }

    public void close() {
        this.status = AccountStatus.CLOSED;
    }

    public void softDelete() {
        this.deleted = true;
    }
}
