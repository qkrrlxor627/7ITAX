package com.ssafy.tax7i.bookentry.dto;

import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookEntryResponse(
        Long id,
        Long paymentId,
        LocalDate entryDate,
        String description,
        String merchantName,
        EntryType entryType,
        Long incomeAmount,
        Long expenseAmount,
        Long fixedAssetAmount,
        Long vatAmount,
        Long supplyPrice,
        String categoryCode,
        String categoryName,
        Boolean isBusinessExpense,
        Boolean isVatDeductible,
        Integer confidenceScore,
        Boolean confirmed,
        LocalDateTime confirmedAt,
        String note,
        LocalDateTime createdAt
) {
    public static BookEntryResponse from(BookEntry entry) {
        return new BookEntryResponse(
                entry.getId(),
                entry.getPaymentId(),
                entry.getEntryDate(),
                entry.getDescription(),
                entry.getMerchantName(),
                entry.getEntryType(),
                entry.getIncomeAmount(),
                entry.getExpenseAmount(),
                entry.getFixedAssetAmount(),
                entry.getVatAmount(),
                entry.getSupplyPrice(),
                entry.getCategoryCode(),
                entry.getCategoryName(),
                entry.getIsBusinessExpense(),
                entry.getIsVatDeductible(),
                entry.getConfidenceScore(),
                entry.getConfirmed(),
                entry.getConfirmedAt(),
                entry.getNote(),
                entry.getCreatedAt()
        );
    }
}
