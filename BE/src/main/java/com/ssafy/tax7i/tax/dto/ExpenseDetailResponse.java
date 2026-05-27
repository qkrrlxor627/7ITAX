package com.ssafy.tax7i.tax.dto;

import com.ssafy.tax7i.tax.entity.ExpenseDetail;

public record ExpenseDetailResponse(
        String expenseCode,
        String expenseName,
        long amount
) {
    public static ExpenseDetailResponse from(ExpenseDetail d) {
        return new ExpenseDetailResponse(
                d.getExpenseCode(),
                d.getExpenseName(),
                d.getAmount()
        );
    }
}
