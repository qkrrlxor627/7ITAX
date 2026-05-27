package com.ssafy.tax7i.auth.dto;

import com.ssafy.tax7i.auth.domain.ConsentType;
import jakarta.validation.constraints.NotNull;

public record ConsentRequest(
        @NotNull(message = "동의 유형은 필수입니다.")
        ConsentType consentType,

        @NotNull(message = "동의 여부는 필수입니다.")
        Boolean agreed
) {
}
