package com.ssafy.tax7i.tax.controller;

import com.ssafy.tax7i.global.response.SuccessResponse;
import com.ssafy.tax7i.tax.dto.VatReturnCreateRequest;
import com.ssafy.tax7i.tax.dto.VatReturnResponse;
import com.ssafy.tax7i.tax.service.VatReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tax/vat-returns")
@RequiredArgsConstructor
public class VatController {

    private final VatReturnService vatReturnService;

    @PostMapping
    public ResponseEntity<SuccessResponse<VatReturnResponse>> createDraft(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VatReturnCreateRequest request) {

        VatReturnResponse response = vatReturnService.createDraft(userId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<List<VatReturnResponse>>> getReturns(
            @AuthenticationPrincipal Long userId) {

        List<VatReturnResponse> response = vatReturnService.getReturns(userId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<VatReturnResponse>> getReturn(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {

        VatReturnResponse response = vatReturnService.getReturn(userId, id);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<SuccessResponse<VatReturnResponse>> submit(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {

        VatReturnResponse response = vatReturnService.submit(userId, id);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
