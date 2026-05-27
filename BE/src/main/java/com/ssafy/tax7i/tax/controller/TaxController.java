package com.ssafy.tax7i.tax.controller;

import com.ssafy.tax7i.global.response.SuccessResponse;
import com.ssafy.tax7i.tax.dto.TaxCalculateRequest;
import com.ssafy.tax7i.tax.dto.TaxCalculationResult;
import com.ssafy.tax7i.tax.dto.TaxPayRequest;
import com.ssafy.tax7i.tax.dto.TaxPayResponse;
import com.ssafy.tax7i.tax.dto.TaxPaymentStatusResponse;
import com.ssafy.tax7i.tax.dto.TaxReturnCreateRequest;
import com.ssafy.tax7i.tax.dto.TaxReturnResponse;
import com.ssafy.tax7i.tax.dto.TaxReturnSubmitResponse;
import com.ssafy.tax7i.tax.dto.TaxReturnUpdateRequest;
import com.ssafy.tax7i.tax.dto.TaxSavingResponse;
import com.ssafy.tax7i.tax.dto.TaxSavingSummaryResponse;
import com.ssafy.tax7i.tax.service.PdfService;
import com.ssafy.tax7i.tax.service.TaxCalculationEngine;
import com.ssafy.tax7i.tax.service.TaxPaymentService;
import com.ssafy.tax7i.tax.service.TaxReturnService;
import com.ssafy.tax7i.tax.service.TaxSavingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tax")
@RequiredArgsConstructor
public class TaxController {

    private final TaxCalculationEngine taxCalculationEngine;
    private final TaxReturnService taxReturnService;
    private final TaxPaymentService taxPaymentService;
    private final TaxSavingService taxSavingService;
    private final PdfService pdfService;

    @PostMapping("/calculate")
    public ResponseEntity<SuccessResponse<TaxCalculationResult>> calculate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TaxCalculateRequest request) {

        TaxCalculationResult result = taxCalculationEngine.calculateForUser(
                userId, request.taxYear(), request.prepaidTax(), request.deductions());

        return ResponseEntity.ok(SuccessResponse.of(result));
    }

    @GetMapping("/savings")
    public ResponseEntity<SuccessResponse<TaxSavingResponse>> getSavingRecommendations(
            @AuthenticationPrincipal Long userId,
            @RequestParam int taxYear) {

        TaxSavingResponse response = taxSavingService.getRecommendations(userId, taxYear);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/savings/summary")
    public ResponseEntity<SuccessResponse<TaxSavingSummaryResponse>> getSavingSummary(
            @AuthenticationPrincipal Long userId,
            @RequestParam int taxYear) {

        TaxSavingSummaryResponse response = taxSavingService.getSummary(userId, taxYear);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    // --- Tax Return endpoints ---

    @PostMapping("/returns")
    public ResponseEntity<SuccessResponse<TaxReturnResponse>> createReturn(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TaxReturnCreateRequest request) {

        TaxReturnResponse response = taxReturnService.createDraft(userId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/returns")
    public ResponseEntity<SuccessResponse<List<TaxReturnResponse>>> getReturns(
            @AuthenticationPrincipal Long userId) {

        List<TaxReturnResponse> response = taxReturnService.getReturns(userId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/returns/{id}")
    public ResponseEntity<SuccessResponse<TaxReturnResponse>> getReturn(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {

        TaxReturnResponse response = taxReturnService.getReturn(userId, id);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PutMapping("/returns/{id}")
    public ResponseEntity<SuccessResponse<TaxReturnResponse>> updateReturn(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody TaxReturnUpdateRequest request) {

        TaxReturnResponse response = taxReturnService.updateReturn(userId, id, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/returns/{id}/submit")
    public ResponseEntity<SuccessResponse<TaxReturnSubmitResponse>> submitReturn(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {

        TaxReturnSubmitResponse response = taxReturnService.submit(userId, id);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/returns/{id}/payment-status")
    public ResponseEntity<SuccessResponse<TaxPaymentStatusResponse>> getPaymentStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {

        TaxPaymentStatusResponse response = taxReturnService.getPaymentStatus(userId, id);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/returns/{id}/pay")
    public ResponseEntity<SuccessResponse<TaxPayResponse>> payNationalTax(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody TaxPayRequest request) {

        TaxPayResponse response = taxPaymentService.payNationalTax(userId, id, request.cardId());
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/returns/{id}/pay-local")
    public ResponseEntity<SuccessResponse<TaxPayResponse>> payLocalTax(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody TaxPayRequest request) {

        TaxPayResponse response = taxPaymentService.payLocalTax(userId, id, request.cardId());
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    // --- PDF endpoints ---

    @GetMapping("/returns/{id}/pdf")
    public ResponseEntity<byte[]> downloadTaxReturnPdf(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {

        byte[] pdf = pdfService.generateTaxReturnPdf(userId, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"tax-return-" + id + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/returns/{id}/receipt")
    public ResponseEntity<byte[]> downloadReceiptPdf(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {

        byte[] pdf = pdfService.generateReceiptPdf(userId, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"tax-receipt-" + id + ".pdf\"")
                .body(pdf);
    }
}
