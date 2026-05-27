package com.ssafy.tax7i.config;

import com.ssafy.tax7i.taxcalendar.entity.TaxDeadline;
import com.ssafy.tax7i.taxcalendar.repository.TaxDeadlineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
public class TaxDeadlineDataInitializer implements CommandLineRunner {

    private final TaxDeadlineRepository taxDeadlineRepository;

    private static final int[] SEED_YEARS = {2025, 2026};

    @Override
    @Transactional
    public void run(String... args) {
        for (int year : SEED_YEARS) {
            initDeadlines(year);
        }
    }

    private void initDeadlines(int year) {
        if (taxDeadlineRepository.countByYear(year) > 0) {
            log.info("[TaxDeadlineDataInitializer] {}년 신고기한 데이터가 이미 존재합니다. 건너뜁니다.", year);
            return;
        }

        log.info("[TaxDeadlineDataInitializer] {}년 세금 신고기한 데이터를 초기화합니다.", year);

        List<TaxDeadline> deadlines = List.of(
                TaxDeadline.builder()
                        .year(year).name("부가가치세 확정신고 (2기)")
                        .description("7~12월 매출·매입 부가세 확정신고 및 납부")
                        .deadlineMonth(1).deadlineDay(25).build(),
                TaxDeadline.builder()
                        .year(year).name("부가가치세 예정신고 (1기)")
                        .description("1~3월 매출·매입 부가세 예정신고 및 납부")
                        .deadlineMonth(4).deadlineDay(25).build(),
                TaxDeadline.builder()
                        .year(year).name("종합소득세 신고")
                        .description("전년도 사업소득 종합소득세 확정신고 및 납부")
                        .deadlineMonth(5).deadlineDay(31).build(),
                TaxDeadline.builder()
                        .year(year).name("지방소득세 신고")
                        .description("종합소득세 기준 지방소득세 신고 및 납부")
                        .deadlineMonth(5).deadlineDay(31).build(),
                TaxDeadline.builder()
                        .year(year).name("부가가치세 확정신고 (1기)")
                        .description("1~6월 매출·매입 부가세 확정신고 및 납부")
                        .deadlineMonth(7).deadlineDay(25).build(),
                TaxDeadline.builder()
                        .year(year).name("부가가치세 예정신고 (2기)")
                        .description("7~9월 매출·매입 부가세 예정신고 및 납부")
                        .deadlineMonth(10).deadlineDay(25).build()
        );

        taxDeadlineRepository.saveAll(deadlines);
        log.info("[TaxDeadlineDataInitializer] {}년 신고기한 {}개 저장 완료.", year, deadlines.size());
    }
}
