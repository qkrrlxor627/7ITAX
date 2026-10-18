package com.ssafy.tax7i.auth.dto;

import com.ssafy.tax7i.card.entity.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 간편 로그인(로컬·성능측정 전용). 전화번호가 곧 사용자 식별자다
 *
 * @param name     처음 로그인하는 번호일 때만 쓰인다(없으면 "사용자{끝4자리}")
 * @param cardType 처음 로그인하는 번호에 발급할 카드 유형(없으면 BUSINESS — 결제 시 장부 자동 생성)
 */
public record SimpleLoginRequest(
        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^01\\d{8,9}$", message = "전화번호는 01로 시작하는 숫자 10~11자리입니다.")
        String phoneNumber,
        @Size(max = 20, message = "이름은 20자 이내입니다.")
        String name,
        CardType cardType
) {}
