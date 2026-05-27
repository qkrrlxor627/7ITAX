package com.ssafy.tax7i.tax.entity;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expense_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseDetail extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_return_id", nullable = false)
    private TaxReturn taxReturn;

    @Column(nullable = false)
    private String expenseCode;

    @Column(nullable = false)
    private String expenseName;

    @Column(nullable = false)
    private long amount;

    @Builder
    public ExpenseDetail(TaxReturn taxReturn, String expenseCode, String expenseName, long amount) {
        this.taxReturn = taxReturn;
        this.expenseCode = expenseCode;
        this.expenseName = expenseName;
        this.amount = amount;
    }
}
