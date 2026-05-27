package com.ssafy.tax7i.tax.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.auth.repository.BusinessProfileRepository;
import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.tax.dto.TaxCalculationResult;
import com.ssafy.tax7i.tax.dto.TaxSavingResponse;
import com.ssafy.tax7i.tax.repository.TaxReturnRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TaxSavingServiceTest {

    @Mock private BookEntryRepository bookEntryRepository;
    @Mock private TaxReturnRepository taxReturnRepository;
    @Mock private TaxCalculationEngine taxCalculationEngine;
    @Mock private TaxParameterService taxParameterService;
    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private TaxSavingService taxSavingService;

    @Test
    @DisplayName("매출 3000만 이하 → 노란우산공제 한도 500만원")
    void recommendations_lowRevenue_noranLimit500() {
        setupMocks(30_000_000L, 10_000_000L, 0.06);

        TaxSavingResponse response = taxSavingService.getRecommendations(1L, 2026);

        assertThat(response.recommendations()).isNotEmpty();
        assertThat(response.recommendations().stream()
                .filter(r -> r.name().contains("노란우산"))
                .findFirst().orElseThrow()
                .maxAmount()).isEqualTo(5_000_000L);
    }

    @Test
    @DisplayName("사업소득금액 6000만 → 노란우산공제 한도 300만원")
    void recommendations_highIncome_noranLimit300() {
        // 매출 8000만 - 경비 2000만 = 사업소득금액 6000만 (4천만 초과, 1억 이하)
        setupMocks(80_000_000L, 20_000_000L, 0.24);

        TaxSavingResponse response = taxSavingService.getRecommendations(1L, 2026);

        assertThat(response.recommendations().stream()
                .filter(r -> r.name().contains("노란우산"))
                .findFirst().orElseThrow()
                .maxAmount()).isEqualTo(3_000_000L);
    }

    @Test
    @DisplayName("접대비 한도 미달 시 추가 사용 가능 안내")
    void recommendations_entertainmentUnderLimit_showsRemaining() {
        setupMocks(50_000_000L, 20_000_000L, 0.15);
        given(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("접대비"), eq(2026)))
                .willReturn(5_000_000L);

        TaxSavingResponse response = taxSavingService.getRecommendations(1L, 2026);

        assertThat(response.recommendations().stream()
                .filter(r -> r.name().contains("접대비"))
                .findFirst()).isPresent();
    }

    @Test
    @DisplayName("접대비 한도 초과 시 접대비 추천 미표시")
    void recommendations_entertainmentOverLimit_noRecommendation() {
        // 매출 5000만 → 한도 = 24,000,000 + 50,000,000 × 0.002 = 24,100,000
        setupMocks(50_000_000L, 20_000_000L, 0.15);
        given(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("접대비"), eq(2026)))
                .willReturn(25_000_000L); // 한도(24,100,000) 초과

        TaxSavingResponse response = taxSavingService.getRecommendations(1L, 2026);

        assertThat(response.recommendations().stream()
                .filter(r -> r.name().contains("접대비"))
                .findFirst()).isEmpty();
    }

    @Test
    @DisplayName("경비불인정 포함 시 businessExpense만 사용")
    void recommendations_nonDeductibleExpense_usesBusinessExpenseOnly() {
        // totalExpense=15M (경비불인정 5M 포함), businessExpense=10M
        given(bookEntryRepository.safeAggregate(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(new AggregateResult(30_000_000L, 15_000_000L, 0L, 10_000_000L));

        // businessExpense(10M)가 calculate에 전달되어야 함 (totalExpense 15M이 아님)
        given(taxCalculationEngine.calculate(anyInt(), eq(30_000_000L), eq(10_000_000L), anyLong(), any()))
                .willReturn(new TaxCalculationResult(
                        30_000_000L, 10_000_000L, 20_000_000L,
                        1_500_000L, 18_500_000L,
                        0.15, 1_515_000L, 0L, 1_515_000L,
                        0L, 1_515_000L, 151_500L, false));

        given(taxParameterService.getBasicDeduction(anyInt())).willReturn(1_500_000L);
        given(taxParameterService.getLocalTaxRate(anyInt())).willReturn(0.10);
        given(taxParameterService.getNoranLimit(anyInt(), anyLong()))
                .willAnswer(inv -> {
                    long businessIncome = inv.getArgument(1);
                    if (businessIncome <= 40_000_000L) return 5_000_000L;
                    else if (businessIncome <= 100_000_000L) return 3_000_000L;
                    else return 2_000_000L;
                });
        given(taxParameterService.getPensionCreditRate(anyInt(), anyLong()))
                .willAnswer(inv -> {
                    long comprehensiveIncome = inv.getArgument(1);
                    return comprehensiveIncome <= 45_000_000L ? 0.15 : 0.12;
                });
        given(taxParameterService.getPensionLimit(anyInt())).willReturn(6_000_000L);
        given(taxParameterService.getEntertainmentLimit(anyInt(), anyLong()))
                .willAnswer(inv -> {
                    long totalRevenue = inv.getArgument(1);
                    return 24_000_000L + (long) Math.floor(totalRevenue * 0.002);
                });
        given(taxParameterService.getVehicleExpenseLimit(anyInt())).willReturn(15_000_000L);
        given(taxParameterService.getEducationLimit(anyInt())).willReturn(1_500_000L);
        lenient().when(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("접대비"), anyInt()))
                .thenReturn(0L);
        lenient().when(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("차량유지비"), anyInt()))
                .thenReturn(0L);
        lenient().when(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("교육훈련비"), anyInt()))
                .thenReturn(0L);
        lenient().when(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("도서인쇄비"), anyInt()))
                .thenReturn(0L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(0L);

        TaxSavingResponse response = taxSavingService.getRecommendations(1L, 2026);

        assertThat(response).isNotNull();
        // businessIncome = 30M - 10M(businessExpense) = 20M → 노란우산 한도 500만
        assertThat(response.recommendations().stream()
                .filter(r -> r.name().contains("노란우산"))
                .findFirst().orElseThrow()
                .maxAmount()).isEqualTo(5_000_000L);
    }

    @Test
    @DisplayName("totalSummary 집계가 올바르게 계산됨")
    void recommendations_totalSummary_notNull() {
        setupMocks(30_000_000L, 10_000_000L, 0.06);

        TaxSavingResponse response = taxSavingService.getRecommendations(1L, 2026);

        assertThat(response.totalSummary()).isNotNull();
        assertThat(response.totalSummary().totalMaxAmount()).isGreaterThan(0);
    }

    private void setupMocks(long income, long expense, double taxRate) {
        given(bookEntryRepository.safeAggregate(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(new AggregateResult(income, expense, 0L, expense));

        given(taxCalculationEngine.calculate(anyInt(), anyLong(), anyLong(), anyLong(), any()))
                .willReturn(new TaxCalculationResult(
                        income, expense, income - expense,
                        1_500_000L, income - expense - 1_500_000L,
                        taxRate, 1_000_000L, 0L, 1_000_000L,
                        0L, 1_000_000L, 100_000L, false));

        // TaxParameterService 모킹
        given(taxParameterService.getBasicDeduction(anyInt())).willReturn(1_500_000L);
        given(taxParameterService.getLocalTaxRate(anyInt())).willReturn(0.10);
        // 사업소득금액(= income - expense) 기준 3단계 (소기업소상공인공제부금법)
        given(taxParameterService.getNoranLimit(anyInt(), anyLong()))
                .willAnswer(inv -> {
                    long businessIncome = inv.getArgument(1);
                    if (businessIncome <= 40_000_000L) return 5_000_000L;
                    else if (businessIncome <= 100_000_000L) return 3_000_000L;
                    else return 2_000_000L;
                });
        // 종합소득금액 기준 (소득세법 §59조의3)
        given(taxParameterService.getPensionCreditRate(anyInt(), anyLong()))
                .willAnswer(inv -> {
                    long comprehensiveIncome = inv.getArgument(1);
                    return comprehensiveIncome <= 45_000_000L ? 0.15 : 0.12;
                });
        given(taxParameterService.getPensionLimit(anyInt())).willReturn(6_000_000L);
        // 접대비 한도: 기본 24M + 수입 × 0.002 (소득세법 §35, 시행령 §78)
        given(taxParameterService.getEntertainmentLimit(anyInt(), anyLong()))
                .willAnswer(inv -> {
                    long totalRevenue = inv.getArgument(1);
                    return 24_000_000L + (long) Math.floor(totalRevenue * 0.002);
                });
        given(taxParameterService.getVehicleExpenseLimit(anyInt())).willReturn(15_000_000L);
        given(taxParameterService.getEducationLimit(anyInt())).willReturn(1_500_000L);

        // 기본: 사용금액 0 (테스트별 오버라이드 가능하도록 lenient)
        lenient().when(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("접대비"), anyInt()))
                .thenReturn(0L);
        lenient().when(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("차량유지비"), anyInt()))
                .thenReturn(0L);
        lenient().when(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("교육훈련비"), anyInt()))
                .thenReturn(0L);
        lenient().when(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(1L), eq("도서인쇄비"), anyInt()))
                .thenReturn(0L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(0L);
    }
}
