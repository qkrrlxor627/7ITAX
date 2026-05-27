package com.ssafy.tax7i.bookentry.dto;

import jakarta.validation.constraints.NotBlank;

public record BookEntryCategoryUpdateRequest(
        @NotBlank(message = "세목 코드는 필수입니다.")
        String categoryCode,

        @NotBlank(message = "세목명은 필수입니다.")
        String categoryName
) {
}
