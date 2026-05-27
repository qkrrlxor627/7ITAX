package com.ssafy.tax7i.transfer.entity;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_transfer_sender_user", columnList = "sender_user_id"),
        @Index(name = "idx_transfer_receiver_user", columnList = "receiver_user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User senderUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id")
    private User receiverUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_card_id", nullable = false)
    private Card senderCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_card_id")
    private Card receiverCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferType transferType;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    private String description;

    private String targetAccountNo;

    private String ssafyTransactionUniqueNo;

    @Column(length = 500)
    private String failureReason;

    @Builder
    public Transfer(User senderUser, User receiverUser, Card senderCard, Card receiverCard,
                    TransferType transferType, Long amount, String description, String targetAccountNo) {
        this.senderUser = senderUser;
        this.receiverUser = receiverUser;
        this.senderCard = senderCard;
        this.receiverCard = receiverCard;
        this.transferType = transferType;
        this.amount = amount;
        this.description = description;
        this.targetAccountNo = targetAccountNo;
        this.status = TransferStatus.COMPLETED;
    }

    public void complete() {
        this.status = TransferStatus.COMPLETED;
    }

    public void fail() {
        this.status = TransferStatus.FAILED;
    }

    public void fail(String reason) {
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
    }

    public void assignSsafyTransactionUniqueNo(String uniqueNo) {
        this.ssafyTransactionUniqueNo = uniqueNo;
    }
}
