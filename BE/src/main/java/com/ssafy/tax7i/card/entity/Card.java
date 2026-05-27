package com.ssafy.tax7i.card.entity;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.global.crypto.AesEncryptor;
import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_card_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String cardName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType cardType;

    @Column(nullable = false, length = 4)
    private String last4Digits;

    @Column(nullable = false)
    private boolean isDefault;

    @Convert(converter = AesEncryptor.class)
    @Column(nullable = false, length = 512)
    private String cardNo;

    @Convert(converter = AesEncryptor.class)
    @Column(nullable = false, length = 512)
    private String cvc;

    @Column(nullable = false)
    private String cardUniqueNo;

    @Column(name = "ssafy_account_no", nullable = false)
    private String ssafyAccountNo;

    private String withdrawalAccountNo;

    private String withdrawalDate;

    private String cardExpiryDate;

    @Column(nullable = false)
    private boolean deleted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status = CardStatus.INACTIVE;

    private String defaultPurpose;

    @Builder
    public Card(User user, String cardName, CardType cardType, String last4Digits,
                String cardNo, String cvc, String cardUniqueNo, String ssafyAccountNo,
                String withdrawalAccountNo, String withdrawalDate, String cardExpiryDate) {
        this.user = user;
        this.cardName = cardName;
        this.cardType = cardType;
        this.last4Digits = last4Digits;
        this.isDefault = false;
        this.cardNo = cardNo;
        this.cvc = cvc;
        this.cardUniqueNo = cardUniqueNo;
        this.ssafyAccountNo = ssafyAccountNo;
        this.withdrawalAccountNo = withdrawalAccountNo;
        this.withdrawalDate = withdrawalDate;
        this.cardExpiryDate = cardExpiryDate;
        this.status = CardStatus.INACTIVE;
    }

    public void markDefault() {
        this.isDefault = true;
    }

    public void unmarkDefault() {
        this.isDefault = false;
    }

    public void activate() {
        this.status = CardStatus.ACTIVE;
    }

    public void updateDefaultPurpose(String purpose) {
        this.defaultPurpose = purpose;
    }

    public void softDelete() {
        this.deleted = true;
        this.isDefault = false;
    }
}
