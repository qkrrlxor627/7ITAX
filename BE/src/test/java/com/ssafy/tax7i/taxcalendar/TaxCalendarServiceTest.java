package com.ssafy.tax7i.taxcalendar;

import com.ssafy.tax7i.taxcalendar.dto.TaxDeadlineResponse;
import com.ssafy.tax7i.taxcalendar.entity.TaxDeadline;
import com.ssafy.tax7i.taxcalendar.repository.TaxDeadlineRepository;
import com.ssafy.tax7i.taxcalendar.service.TaxCalendarService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TaxCalendarServiceTest {

    @Mock
    private TaxDeadlineRepository taxDeadlineRepository;

    @InjectMocks
    private TaxCalendarService service;

    private static final List<TaxDeadline> DEADLINES = List.of(
            TaxDeadline.builder().year(2025).name("부가가치세 확정신고 (2기)")
                    .description("7~12월 매출·매입 부가세 확정신고 및 납부").deadlineMonth(1).deadlineDay(25).build(),
            TaxDeadline.builder().year(2025).name("부가가치세 예정신고 (1기)")
                    .description("1~3월 매출·매입 부가세 예정신고 및 납부").deadlineMonth(4).deadlineDay(25).build(),
            TaxDeadline.builder().year(2025).name("종합소득세 신고")
                    .description("전년도 사업소득 종합소득세 확정신고 및 납부").deadlineMonth(5).deadlineDay(31).build(),
            TaxDeadline.builder().year(2025).name("지방소득세 신고")
                    .description("종합소득세 기준 지방소득세 신고 및 납부").deadlineMonth(5).deadlineDay(31).build(),
            TaxDeadline.builder().year(2025).name("부가가치세 확정신고 (1기)")
                    .description("1~6월 매출·매입 부가세 확정신고 및 납부").deadlineMonth(7).deadlineDay(25).build(),
            TaxDeadline.builder().year(2025).name("부가가치세 예정신고 (2기)")
                    .description("7~9월 매출·매입 부가세 예정신고 및 납부").deadlineMonth(10).deadlineDay(25).build()
    );

    @Test
    @DisplayName("D-day 계산: 모든 세금 일정이 미래 날짜로 반환된다")
    void allDeadlinesAreFutureOrToday() {
        LocalDate today = LocalDate.of(2026, 3, 17);
        given(taxDeadlineRepository.findByYear(2026)).willReturn(DEADLINES);

        List<TaxDeadlineResponse> deadlines = service.getUpcomingDeadlines(today);

        assertThat(deadlines).isNotEmpty();
        for (TaxDeadlineResponse d : deadlines) {
            assertThat(d.dDay()).isGreaterThanOrEqualTo(0);
            assertThat(LocalDate.parse(d.deadline())).isAfterOrEqualTo(today);
        }
    }

    @Test
    @DisplayName("D-day 계산: 결과는 D-day 오름차순 정렬")
    void deadlinesSortedByDDay() {
        LocalDate today = LocalDate.of(2026, 3, 17);
        given(taxDeadlineRepository.findByYear(2026)).willReturn(DEADLINES);

        List<TaxDeadlineResponse> deadlines = service.getUpcomingDeadlines(today);

        for (int i = 1; i < deadlines.size(); i++) {
            assertThat(deadlines.get(i).dDay()).isGreaterThanOrEqualTo(deadlines.get(i - 1).dDay());
        }
    }

    @Test
    @DisplayName("D-day 계산: 원천세는 목록에 포함되지 않는다")
    void withholddingTaxNotIncluded() {
        LocalDate today = LocalDate.of(2026, 3, 17);
        given(taxDeadlineRepository.findByYear(2026)).willReturn(DEADLINES);

        List<TaxDeadlineResponse> deadlines = service.getUpcomingDeadlines(today);

        boolean hasWithholding = deadlines.stream()
                .anyMatch(d -> d.taxName().contains("원천세"));
        assertThat(hasWithholding).isFalse();
    }

    @Test
    @DisplayName("D-day 계산: 총 6개 일정 반환")
    void totalSixSchedules() {
        LocalDate today = LocalDate.of(2026, 3, 17);
        given(taxDeadlineRepository.findByYear(2026)).willReturn(DEADLINES);

        List<TaxDeadlineResponse> deadlines = service.getUpcomingDeadlines(today);
        assertThat(deadlines).hasSize(6);
    }

    @Test
    @DisplayName("D-day 계산: 부가세 예정신고(4/25) 포함")
    void vatPreReportIncluded() {
        LocalDate today = LocalDate.of(2026, 3, 17);
        given(taxDeadlineRepository.findByYear(2026)).willReturn(DEADLINES);

        List<TaxDeadlineResponse> deadlines = service.getUpcomingDeadlines(today);

        boolean hasVatPre = deadlines.stream()
                .anyMatch(d -> d.taxName().contains("부가가치세 예정신고 (1기)") &&
                        d.deadline().equals("2026-04-25"));
        assertThat(hasVatPre).isTrue();
    }

    @Test
    @DisplayName("D-day 계산: 종소세(5/31) D-day 정확")
    void incomeTaxDDay() {
        LocalDate today = LocalDate.of(2026, 5, 1);
        given(taxDeadlineRepository.findByYear(2026)).willReturn(DEADLINES);

        List<TaxDeadlineResponse> deadlines = service.getUpcomingDeadlines(today);

        TaxDeadlineResponse incomeTax = deadlines.stream()
                .filter(d -> d.taxName().contains("종합소득세"))
                .findFirst().orElseThrow();

        assertThat(incomeTax.dDay()).isEqualTo(30L);
    }

    @Test
    @DisplayName("D-day 계산: 이미 지난 일정은 다음 연도로 넘어간다")
    void pastDeadlineRollsToNextYear() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        given(taxDeadlineRepository.findByYear(2026)).willReturn(DEADLINES);

        List<TaxDeadlineResponse> deadlines = service.getUpcomingDeadlines(today);

        TaxDeadlineResponse incomeTax = deadlines.stream()
                .filter(d -> d.taxName().contains("종합소득세"))
                .findFirst().orElseThrow();

        // 5/31이 지났으므로 2027년
        assertThat(LocalDate.parse(incomeTax.deadline()).getYear()).isEqualTo(2027);
    }
}
