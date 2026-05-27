package com.ssafy.tax7i.taxcalendar.service;

import com.ssafy.tax7i.taxcalendar.dto.TaxDeadlineResponse;
import com.ssafy.tax7i.taxcalendar.entity.TaxDeadline;
import com.ssafy.tax7i.taxcalendar.repository.TaxDeadlineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxCalendarService {

    private final TaxDeadlineRepository taxDeadlineRepository;

    @Cacheable(value = "taxDeadlines", key = "#today")
    public List<TaxDeadlineResponse> getUpcomingDeadlines(LocalDate today) {
        List<TaxDeadlineResponse> result = new ArrayList<>();
        int year = today.getYear();

        // 현재 연도 데이터 조회, 없으면 이전 연도 폴백
        List<TaxDeadline> deadlines = taxDeadlineRepository.findByYear(year);
        if (deadlines.isEmpty()) {
            deadlines = taxDeadlineRepository.findByYear(year - 1);
        }

        for (TaxDeadline d : deadlines) {
            LocalDate deadline = LocalDate.of(year, d.getDeadlineMonth(), d.getDeadlineDay());
            if (deadline.isBefore(today)) {
                deadline = deadline.plusYears(1);
            }
            result.add(toResponse(d, deadline, today));
        }

        result.sort(Comparator.comparingInt(TaxDeadlineResponse::dDay));
        return result;
    }

    private TaxDeadlineResponse toResponse(TaxDeadline d, LocalDate deadline, LocalDate today) {
        int dDay = (int) ChronoUnit.DAYS.between(today, deadline);
        return new TaxDeadlineResponse(d.getName(), d.getDescription(), deadline.toString(), dDay);
    }
}
