package com.ssafy.tax7i.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IdentityVerifyRequest(
        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "생년월일은 필수입니다.")
        String birthDate,

        @NotBlank(message = "성별은 필수입니다.")
        String gender,

        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "유효한 휴대폰 번호 형식이 아닙니다.")
        String phoneNumber
) {}
