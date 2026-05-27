package com.ssafy.tax7i.taxcalendar.controller;

import com.ssafy.tax7i.global.response.SuccessResponse;
import com.ssafy.tax7i.taxcalendar.dto.TaxDeadlineResponse;
import com.ssafy.tax7i.taxcalendar.service.TaxCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tax-calendar")
@RequiredArgsConstructor
public class TaxCalendarController {

    private final TaxCalendarService taxCalendarService;

    @GetMapping("/deadlines")
    public ResponseEntity<SuccessResponse<List<TaxDeadlineResponse>>> getDeadlines() {
        List<TaxDeadlineResponse> deadlines = taxCalendarService.getUpcomingDeadlines(LocalDate.now());
        return ResponseEntity.ok(SuccessResponse.of(deadlines));
    }
}
