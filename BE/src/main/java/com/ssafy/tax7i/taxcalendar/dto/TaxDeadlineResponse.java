package com.ssafy.tax7i.taxcalendar.dto;

public record TaxDeadlineResponse(
        String taxName,
        String description,
        String deadline,
        int dDay
) {
}
