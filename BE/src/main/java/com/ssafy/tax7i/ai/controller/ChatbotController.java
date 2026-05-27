package com.ssafy.tax7i.ai.controller;

import com.ssafy.tax7i.ai.dto.ChatHistoryResponse;
import com.ssafy.tax7i.ai.dto.ChatbotRequest;
import com.ssafy.tax7i.ai.dto.ChatbotResponse;
import com.ssafy.tax7i.ai.service.ChatbotService;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private static final int MAX_HISTORY_SIZE = 50;

    private final ChatbotService chatbotService;

    @PostMapping
    public ResponseEntity<SuccessResponse<ChatbotResponse>> sendMessage(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChatbotRequest request) {
        ChatbotResponse response = chatbotService.sendMessage(userId, request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<SuccessResponse<ChatHistoryResponse>> getHistory(
            @AuthenticationPrincipal Long userId,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(size, MAX_HISTORY_SIZE);
        ChatHistoryResponse response = chatbotService.getHistory(userId, sessionId, page, safeSize);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
