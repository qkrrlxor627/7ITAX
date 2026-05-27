package com.ssafy.tax7i.taxestimation.controller;

import com.ssafy.tax7i.config.TestSecurityConfig;
import com.ssafy.tax7i.taxestimation.dto.MonthlyTaxEstimationResponse;
import com.ssafy.tax7i.taxestimation.dto.TaxEstimationResponse;
import com.ssafy.tax7i.taxestimation.service.TaxEstimationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaxEstimationController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class TaxEstimationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TaxEstimationService taxEstimationService;
    @MockitoBean private com.ssafy.tax7i.global.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    // ───────────── GET /api/tax-estimation ─────────────

    @Test
    void estimate_200_세금추정() throws Exception {
        TaxEstimationResponse response = buildTaxEstimationResponse(2026);
        given(taxEstimationService.estimate(any(), any(Integer.class))).willReturn(response);

        mockMvc.perform(get("/api/tax-estimation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.totalIncome").value(50_000_000))
                .andExpect(jsonPath("$.data.totalExpense").value(20_000_000))
                .andExpect(jsonPath("$.data.taxableIncome").value(30_000_000))
                .andExpect(jsonPath("$.data.estimatedVat").value(2_727_273))
                .andExpect(jsonPath("$.data.estimatedIncomeTax").value(3_240_000))
                .andExpect(jsonPath("$.data.incomeTaxBracket").value("15%"))
                .andExpect(jsonPath("$.data.estimatedLocalTax").value(324_000))
                .andExpect(jsonPath("$.data.totalAnnualTax").isNumber())
                .andExpect(jsonPath("$.data.vatByPeriod").exists())
                .andExpect(jsonPath("$.data.bracketDetail").exists());
    }

    @Test
    void estimate_200_연도파라미터() throws Exception {
        TaxEstimationResponse response = buildTaxEstimationResponse(2025);
        given(taxEstimationService.estimate(any(), eq(2025))).willReturn(response);

        mockMvc.perform(get("/api/tax-estimation")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.year").value(2025));
    }

    // ───────────── GET /api/tax-estimation/monthly ─────────────

    @Test
    void estimateMonthly_200_월별추정() throws Exception {
        MonthlyTaxEstimationResponse response = buildMonthlyResponse(2026, 3);
        given(taxEstimationService.estimateMonthly(any(), eq(2026), eq(3))).willReturn(response);

        mockMvc.perform(get("/api/tax-estimation/monthly")
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(3))
                .andExpect(jsonPath("$.data.estimatedVat").exists())
                .andExpect(jsonPath("$.data.estimatedVat.period").value("1기"))
                .andExpect(jsonPath("$.data.estimatedVat.estimatedPayable").value(2_000_000))
                .andExpect(jsonPath("$.data.estimatedIncomeTax").exists())
                .andExpect(jsonPath("$.data.estimatedLocalTax").exists())
                .andExpect(jsonPath("$.data.totalEstimatedTax").isNumber());
    }

    @Test
    void estimateMonthly_400_잘못된연도() throws Exception {
        mockMvc.perform(get("/api/tax-estimation/monthly")
                        .param("year", "1800")
                        .param("month", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    @Test
    void estimateMonthly_400_잘못된월() throws Exception {
        mockMvc.perform(get("/api/tax-estimation/monthly")
                        .param("year", "2026")
                        .param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    // ───────────── helpers ─────────────

    private TaxEstimationResponse buildTaxEstimationResponse(int year) {
        TaxEstimationResponse.VatPeriodDetail period1 = new TaxEstimationResponse.VatPeriodDetail(
                "1기 (1월~6월)", 2_272_727L, 909_091L, 1_363_636L, year + "-07-25");
        TaxEstimationResponse.VatPeriodDetail period2 = new TaxEstimationResponse.VatPeriodDetail(
                "2기 (7월~12월)", 2_272_728L, 909_091L, 1_363_637L, (year + 1) + "-01-25");
        TaxEstimationResponse.VatByPeriod vatByPeriod = new TaxEstimationResponse.VatByPeriod(
                period1, period2, 2_727_273L);
        TaxEstimationResponse.BracketDetail bracketDetail = new TaxEstimationResponse.BracketDetail(
                "15%", 14_000_000L, 50_000_000L, 30_000_000L, 20_000_000L, "24%");

        return new TaxEstimationResponse(
                year,
                50_000_000L,
                20_000_000L,
                0L,
                30_000_000L,
                4_545_455L,
                1_818_182L,
                2_727_273L,
                3_240_000L,
                "15%",
                324_000L,
                20_000_000L,
                486_000L,
                vatByPeriod,
                6_291_273L,
                bracketDetail
        );
    }

    private MonthlyTaxEstimationResponse buildMonthlyResponse(int year, int month) {
        MonthlyTaxEstimationResponse.VatEstimation vatEstimation =
                new MonthlyTaxEstimationResponse.VatEstimation(
                        "1기", 3_000_000L, 1_000_000L, 2_000_000L, year + "-07-25");
        MonthlyTaxEstimationResponse.IncomeTaxEstimation incomeTaxEstimation =
                new MonthlyTaxEstimationResponse.IncomeTaxEstimation(
                        15_000_000L, 6_000_000L, 60_000_000L, 24_000_000L, 4_290_000L, (year + 1) + "-05-31");
        MonthlyTaxEstimationResponse.LocalTaxEstimation localTaxEstimation =
                new MonthlyTaxEstimationResponse.LocalTaxEstimation(429_000L, (year + 1) + "-05-31");

        return new MonthlyTaxEstimationResponse(
                year, month, vatEstimation, incomeTaxEstimation, localTaxEstimation, 6_719_000L);
    }
}
