package com.ssafy.tax7i.classification.controller;

import com.ssafy.tax7i.ai.service.AiRateLimiter;
import com.ssafy.tax7i.classification.dto.ClassificationRequest;
import com.ssafy.tax7i.classification.dto.ClassificationResult;
import com.ssafy.tax7i.classification.service.TaxClassificationService;
import com.ssafy.tax7i.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classification")
@RequiredArgsConstructor
public class ClassificationController {

    private final TaxClassificationService classificationService;
    private final AiRateLimiter rateLimiter;

    @PostMapping
    public ResponseEntity<SuccessResponse<ClassificationResult>> classify(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ClassificationRequest request) {

        rateLimiter.checkClassifyLimit(userId);

        ClassificationRequest withUser = new ClassificationRequest(
                request.merchantName(), request.mcc(), request.amount(),
                request.isBusinessPurpose(), request.isClientAccompanied(), userId,
                request.note());

        ClassificationResult result = classificationService.classify(withUser);
        return ResponseEntity.ok(SuccessResponse.of(result));
    }
}
