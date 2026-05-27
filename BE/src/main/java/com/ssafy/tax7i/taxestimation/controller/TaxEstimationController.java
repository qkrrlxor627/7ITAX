package com.ssafy.tax7i.taxestimation.controller;

import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.global.response.SuccessResponse;
import com.ssafy.tax7i.taxestimation.dto.MonthlyTaxEstimationResponse;
import com.ssafy.tax7i.taxestimation.dto.TaxEstimationResponse;
import com.ssafy.tax7i.taxestimation.service.TaxEstimationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/tax-estimation")
@RequiredArgsConstructor
public class TaxEstimationController {

    private final TaxEstimationService taxEstimationService;

    @GetMapping
    public ResponseEntity<SuccessResponse<TaxEstimationResponse>> estimate(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        TaxEstimationResponse response = taxEstimationService.estimate(userId, targetYear);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/monthly")
    public ResponseEntity<SuccessResponse<MonthlyTaxEstimationResponse>> estimateMonthly(
            @AuthenticationPrincipal Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "연도는 1900~2099 범위여야 합니다.");
        }
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "월은 1~12 범위여야 합니다.");
        }
        MonthlyTaxEstimationResponse response = taxEstimationService.estimateMonthly(userId, year, month);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
