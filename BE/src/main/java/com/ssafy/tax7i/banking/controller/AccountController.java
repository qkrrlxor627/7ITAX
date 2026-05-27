package com.ssafy.tax7i.banking.controller;

import com.ssafy.tax7i.banking.dto.AccountCreateRequest;
import com.ssafy.tax7i.banking.dto.AccountResponse;
import com.ssafy.tax7i.banking.dto.BalanceResponse;
import com.ssafy.tax7i.banking.service.AccountService;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banking/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<SuccessResponse<AccountResponse>> createAccount(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AccountCreateRequest request) {
        AccountResponse response = accountService.createAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<List<AccountResponse>>> getAccounts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String accountType) {
        List<AccountResponse> response = accountService.getAccounts(userId, accountType);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<SuccessResponse<AccountResponse>> getAccount(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long accountId) {
        AccountResponse response = accountService.getAccount(userId, accountId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<SuccessResponse<BalanceResponse>> getBalance(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long accountId) {
        BalanceResponse response = accountService.getBalance(userId, accountId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
