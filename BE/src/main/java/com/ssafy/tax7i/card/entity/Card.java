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
        @Index(name = "idx_card_user_id", columnList = "user_id"),
        @Index(name = "idx_card_no_key", columnList = "card_no_key")
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

    /**
     * 평문 조회 키. cardNo는 AES/GCM(랜덤 IV)으로 암호화되어 매번 암호문이 달라
     * 컬럼으로 조회할 수 없으므로, 평문 인덱스 키를 별도로 보관해 조회에 사용한다.
     */
    @Column(name = "card_no_key", unique = true)
    private String cardNoKey;

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
        // 평문 조회 키를 빌더 생성 시점에 자동 복사 (CardService 변경 불필요)
        this.cardNoKey = cardNo;
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
