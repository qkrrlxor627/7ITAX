package com.ssafy.tax7i.classification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ClassificationRequest(
        @NotBlank(message = "가맹점명은 필수입니다.")
        String merchantName,

        String mcc, // nullable — 없으면 가맹점명으로 MCC 조회

        @Positive(message = "금액은 0보다 커야 합니다.")
        Long amount,

        Boolean isBusinessPurpose, // 사업용 여부 (Tier B 조건)

        Boolean isClientAccompanied, // 거래처 동행 여부 (접대비 판단)

        Long userId, // 접대비 누적 한도 체크용

        String note // 증빙 메모 — AI 분류 시 description에 합쳐서 전달
) {
}
