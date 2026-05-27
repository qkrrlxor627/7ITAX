package com.ssafy.tax7i.card.dto;

import java.time.LocalDateTime;

public record CardPurposeResponse(
        Long id,
        String defaultPurpose,
        LocalDateTime updatedAt
) {}
