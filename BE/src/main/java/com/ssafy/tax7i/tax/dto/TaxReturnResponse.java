package com.ssafy.tax7i.tax.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.tax.entity.ExpenseDetail;
import com.ssafy.tax7i.tax.entity.TaxReturn;
import com.ssafy.tax7i.tax.entity.TaxReturnStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record TaxReturnResponse(
        Long id,
        Long userId,
        int taxYear,
        TaxReturnStatus status,
        String receiptNumber,
        String taxpayerName,
        long totalRevenue,
        long totalExpense,
        long incomeAmount,
        long totalDeductions,
        long taxableIncome,
        double taxRate,
        long calculatedTax,
        long determinedTax,
        long prepaidTax,
        long finalTax,
        long localTax,
        Map<String, Long> deductions,
        List<ExpenseDetailResponse> expenseDetails,
        LocalDateTime submittedAt,
        LocalDateTime acceptedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static TaxReturnResponse from(TaxReturn r, List<ExpenseDetail> details) {
        Map<String, Long> deductions = parseDeductions(r.getDeductionsJson());
        List<ExpenseDetailResponse> expenseDetails = details.stream()
                .map(ExpenseDetailResponse::from)
                .toList();

        return new TaxReturnResponse(
                r.getId(),
                r.getUserId(),
                r.getTaxYear(),
                r.getStatus(),
                r.getReceiptNumber(),
                r.getTaxpayerName(),
                r.getTotalRevenue(),
                r.getTotalExpense(),
                r.getIncomeAmount(),
                r.getTotalDeductions(),
                r.getTaxableIncome(),
                r.getTaxRate(),
                r.getCalculatedTax(),
                r.getDeterminedTax(),
                r.getPrepaidTax(),
                r.getFinalTax(),
                r.getLocalTax(),
                deductions,
                expenseDetails,
                r.getSubmittedAt(),
                r.getAcceptedAt(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private static Map<String, Long> parseDeductions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Long>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
