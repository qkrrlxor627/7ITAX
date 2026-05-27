package com.ssafy.tax7i.bookentry.entity;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "book_entries", indexes = {
        @Index(name = "idx_be_user_date", columnList = "user_id, entry_date DESC"),
        @Index(name = "idx_be_user_confirmed", columnList = "user_id, confirmed")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookEntry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(unique = true)
    private Long paymentId;

    @Column(nullable = false)
    private LocalDate entryDate;

    @Column(length = 500)
    private String description;

    @Column(length = 200)
    private String merchantName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryType entryType;

    @Column(nullable = false)
    private Long incomeAmount;

    @Column(nullable = false)
    private Long expenseAmount;

    @Column(nullable = false)
    private Long fixedAssetAmount;

    @Column(nullable = false)
    private Long vatAmount;

    @Column(nullable = false)
    private Long supplyPrice;

    @Column(length = 10)
    private String categoryCode;

    @Column(length = 50)
    private String categoryName;

    @Column(nullable = false)
    private Boolean isBusinessExpense;

    @Column(nullable = false)
    private Boolean isVatDeductible;

    @Column(nullable = false)
    private Integer confidenceScore;

    @Column(nullable = false)
    private Boolean confirmed;

    private LocalDateTime confirmedAt;

    @Column(length = 500)
    private String note;

    @Builder
    public BookEntry(Long userId, Long paymentId, LocalDate entryDate,
                     String description, String merchantName, EntryType entryType,
                     Long incomeAmount, Long expenseAmount, Long fixedAssetAmount,
                     Long vatAmount, Long supplyPrice,
                     String categoryCode, String categoryName,
                     Boolean isVatDeductible, Integer confidenceScore, String note) {
        this.userId = userId;
        this.paymentId = paymentId;
        this.entryDate = entryDate;
        this.description = description;
        this.merchantName = merchantName;
        this.entryType = entryType;
        this.incomeAmount = incomeAmount != null ? incomeAmount : 0L;
        this.expenseAmount = expenseAmount != null ? expenseAmount : 0L;
        this.fixedAssetAmount = fixedAssetAmount != null ? fixedAssetAmount : 0L;
        this.vatAmount = vatAmount != null ? vatAmount : 0L;
        this.supplyPrice = supplyPrice != null ? supplyPrice : 0L;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.isBusinessExpense = true;
        this.isVatDeductible = isVatDeductible != null ? isVatDeductible : true;
        this.confidenceScore = confidenceScore != null ? confidenceScore : 0;
        this.confirmed = false;
        this.note = note;
    }

    public void confirm() {
        if (this.confirmed) {
            throw new BusinessException(ErrorCode.ALREADY_CONFIRMED);
        }
        this.confirmed = true;
        this.confirmedAt = LocalDateTime.now();
    }

    public void updateCategory(String categoryCode, String categoryName) {
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "세목 코드는 비어있을 수 없습니다.");
        }
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
    }

    public void markAsPersonal() {
        this.isBusinessExpense = false;
    }

    public void markAsBusiness() {
        this.isBusinessExpense = true;
    }

    public void updateNote(String note) {
        this.note = (note != null && note.isBlank()) ? null : note;
    }
}
