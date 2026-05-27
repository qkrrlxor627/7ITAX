package com.ssafy.tax7i.transfer.controller;

import com.ssafy.tax7i.global.response.SuccessResponse;
import com.ssafy.tax7i.transfer.dto.P2pTransferRequest;
import com.ssafy.tax7i.transfer.dto.TransferResponse;
import com.ssafy.tax7i.transfer.dto.WithdrawRequest;
import com.ssafy.tax7i.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/p2p")
    public ResponseEntity<SuccessResponse<TransferResponse>> p2pTransfer(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody P2pTransferRequest request) {
        TransferResponse response = transferService.p2pTransfer(userId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<SuccessResponse<TransferResponse>> withdraw(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WithdrawRequest request) {
        TransferResponse response = transferService.withdraw(userId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<Page<TransferResponse>>> getTransfers(
            @AuthenticationPrincipal Long userId,
            Pageable pageable) {
        Page<TransferResponse> response = transferService.getTransfers(userId, pageable);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<SuccessResponse<TransferResponse>> getTransfer(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transferId) {
        TransferResponse response = transferService.getTransfer(userId, transferId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
