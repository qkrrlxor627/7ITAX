package com.ssafy.tax7i.card.dto;

import jakarta.validation.constraints.NotBlank;

public record CardActivateRequest(
        @NotBlank String activationCode
) {}
