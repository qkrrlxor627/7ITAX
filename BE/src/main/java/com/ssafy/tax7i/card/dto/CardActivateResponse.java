package com.ssafy.tax7i.card.dto;

import java.time.LocalDateTime;

public record CardActivateResponse(
        Long id,
        String status,
        LocalDateTime activatedAt
) {}
