package com.ssafy.tax7i.card.controller;

import com.ssafy.tax7i.card.dto.*;
import com.ssafy.tax7i.card.service.CardService;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * 가맹점 목록 조회
     */
    @GetMapping("/merchants")
    public ResponseEntity<SuccessResponse<List<MerchantResponse>>> getMerchants(
            @AuthenticationPrincipal Long userId) {
        List<MerchantResponse> response = cardService.getMerchants(userId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    /**
     * 내 계좌 목록 조회
     */
    @GetMapping("/accounts")
    public ResponseEntity<SuccessResponse<List<AccountResponse>>> getMyAccounts(
            @AuthenticationPrincipal Long userId) {
        List<AccountResponse> response = cardService.getMyAccounts(userId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    /**
     * 카드 상품 목록 조회
     */
    @GetMapping("/products")
    public ResponseEntity<SuccessResponse<List<CardProductResponse>>> getCardProducts(
            @AuthenticationPrincipal Long userId) {
        List<CardProductResponse> response = cardService.getCardProducts(userId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    /**
     * 신용카드 발급
     */
    @PostMapping
    public ResponseEntity<SuccessResponse<CardResponse>> createCard(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateCardRequest request) {
        CardResponse response = cardService.createCard(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.of(response));
    }

    /**
     * 내 카드 목록 조회
     */
    @GetMapping
    public ResponseEntity<SuccessResponse<List<CardResponse>>> getCards(
            @AuthenticationPrincipal Long userId) {
        List<CardResponse> response = cardService.getCards(userId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    /**
     * 카드 상세 조회
     */
    @GetMapping("/{cardId}")
    public ResponseEntity<SuccessResponse<CardResponse>> getCard(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId) {
        CardResponse response = cardService.getCard(userId, cardId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    /**
     * 기본 카드 설정
     */
    @PatchMapping("/{cardId}/default")
    public ResponseEntity<SuccessResponse<CardResponse>> setDefault(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId) {
        CardResponse response = cardService.setDefaultCard(userId, cardId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    /**
     * 카드 삭제
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<SuccessResponse<Void>> deleteCard(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId) {
        cardService.deleteCard(userId, cardId);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    /**
     * 카드 활성화
     */
    @PostMapping("/{cardId}/activate")
    public ResponseEntity<SuccessResponse<CardActivateResponse>> activateCard(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId,
            @RequestBody @Valid CardActivateRequest request) {
        CardActivateResponse response = cardService.activateCard(userId, cardId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    /**
     * 카드 용도 변경
     */
    @PatchMapping("/{cardId}/purpose")
    public ResponseEntity<SuccessResponse<CardPurposeResponse>> setCardPurpose(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId,
            @RequestBody @Valid CardPurposeRequest request) {
        CardPurposeResponse response = cardService.setCardPurpose(userId, cardId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    /**
     * 카드 결제
     */
    @PostMapping("/{cardId}/payment")
    public ResponseEntity<SuccessResponse<CardPaymentResponse>> payment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId,
            @Valid @RequestBody CardPaymentRequest request) {
        CardPaymentResponse response = cardService.payment(userId, cardId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    /**
     * 카드 결제 취소
     */
    @PostMapping("/{cardId}/payment/cancel")
    public ResponseEntity<SuccessResponse<Void>> cancelPayment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId,
            @Valid @RequestBody CancelPaymentRequest request) {
        cardService.cancelPayment(userId, cardId, request);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    /**
     * 카드 결제 내역 조회
     */
    @GetMapping("/{cardId}/transactions")
    public ResponseEntity<SuccessResponse<List<CardTransactionResponse>>> getTransactions(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        List<CardTransactionResponse> response = cardService.getTransactions(userId, cardId, startDate, endDate);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    /**
     * 청구서 조회
     */
    @GetMapping("/{cardId}/billing")
    public ResponseEntity<SuccessResponse<List<CardBillingResponse>>> getBillingStatements(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cardId,
            @RequestParam String startMonth,
            @RequestParam String endMonth) {
        List<CardBillingResponse> response = cardService.getBillingStatements(userId, cardId, startMonth, endMonth);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
