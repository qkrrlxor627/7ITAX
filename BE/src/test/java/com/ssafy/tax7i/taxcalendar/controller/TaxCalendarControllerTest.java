package com.ssafy.tax7i.taxcalendar.controller;

import com.ssafy.tax7i.config.TestSecurityConfig;
import com.ssafy.tax7i.taxcalendar.dto.TaxDeadlineResponse;
import com.ssafy.tax7i.taxcalendar.service.TaxCalendarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaxCalendarController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class TaxCalendarControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TaxCalendarService taxCalendarService;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ───────────── GET /api/tax-calendar/deadlines ─────────────

    @Test
    void getDeadlines_200_마감일목록() throws Exception {
        List<TaxDeadlineResponse> deadlines = List.of(
                new TaxDeadlineResponse("부가가치세 예정신고 (1기)", "1~3월 매출·매입 부가세 예정신고 및 납부", "2026-04-25", 30),
                new TaxDeadlineResponse("종합소득세 신고", "전년도 사업소득 종합소득세 확정신고 및 납부", "2026-05-31", 66),
                new TaxDeadlineResponse("부가가치세 확정신고 (1기)", "1~6월 매출·매입 부가세 확정신고 및 납부", "2026-07-25", 121)
        );
        given(taxCalendarService.getUpcomingDeadlines(any())).willReturn(deadlines);

        mockMvc.perform(get("/api/tax-calendar/deadlines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].taxName").value("부가가치세 예정신고 (1기)"))
                .andExpect(jsonPath("$.data[0].deadline").value("2026-04-25"))
                .andExpect(jsonPath("$.data[0].dDay").value(30))
                .andExpect(jsonPath("$.data[1].taxName").value("종합소득세 신고"))
                .andExpect(jsonPath("$.data[2].taxName").value("부가가치세 확정신고 (1기)"));
    }

    @Test
    void getDeadlines_200_정렬확인() throws Exception {
        // dDay 오름차순 정렬 검증 (서비스가 정렬하여 반환)
        List<TaxDeadlineResponse> sorted = List.of(
                new TaxDeadlineResponse("부가가치세 예정신고 (1기)", "1~3월 매출·매입 부가세 예정신고 및 납부", "2026-04-25", 30),
                new TaxDeadlineResponse("종합소득세 신고", "전년도 사업소득 종합소득세 확정신고 및 납부", "2026-05-31", 66),
                new TaxDeadlineResponse("지방소득세 신고", "종합소득세 기준 지방소득세 신고 및 납부", "2026-05-31", 66),
                new TaxDeadlineResponse("부가가치세 확정신고 (1기)", "1~6월 매출·매입 부가세 확정신고 및 납부", "2026-07-25", 121),
                new TaxDeadlineResponse("부가가치세 예정신고 (2기)", "7~9월 매출·매입 부가세 예정신고 및 납부", "2026-10-25", 213),
                new TaxDeadlineResponse("부가가치세 확정신고 (2기)", "7~12월 매출·매입 부가세 확정신고 및 납부", "2027-01-25", 305)
        );
        given(taxCalendarService.getUpcomingDeadlines(any())).willReturn(sorted);

        mockMvc.perform(get("/api/tax-calendar/deadlines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(6))
                // dDay 오름차순: 30 <= 66 <= 66 <= 121 <= 213 <= 305
                .andExpect(jsonPath("$.data[0].dDay").value(30))
                .andExpect(jsonPath("$.data[1].dDay").value(66))
                .andExpect(jsonPath("$.data[2].dDay").value(66))
                .andExpect(jsonPath("$.data[3].dDay").value(121))
                .andExpect(jsonPath("$.data[4].dDay").value(213))
                .andExpect(jsonPath("$.data[5].dDay").value(305));
    }
}
