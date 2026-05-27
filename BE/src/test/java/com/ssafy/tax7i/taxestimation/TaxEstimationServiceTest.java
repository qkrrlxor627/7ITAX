package com.ssafy.tax7i.taxestimation;

import com.ssafy.tax7i.auth.repository.BusinessProfileRepository;
import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.tax.entity.TaxBracket;
import com.ssafy.tax7i.tax.service.TaxCalculationEngine;
import com.ssafy.tax7i.tax.service.TaxParameterService;
import com.ssafy.tax7i.taxcalendar.repository.TaxDeadlineRepository;
import com.ssafy.tax7i.taxestimation.dto.MonthlyTaxEstimationResponse;
import com.ssafy.tax7i.taxestimation.dto.TaxEstimationResponse;
import com.ssafy.tax7i.taxestimation.service.TaxEstimationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TaxEstimationServiceTest {

    @Mock
    private BookEntryRepository bookEntryRepository;

    @Mock
    private TaxCalculationEngine taxCalculationEngine;

    @Mock
    private TaxParameterService taxParameterService;

    @Mock
    private TaxDeadlineRepository taxDeadlineRepository;

    @Mock
    private BusinessProfileRepository businessProfileRepository;

    @InjectMocks
    private TaxEstimationService taxEstimationService;

    private static final List<TaxBracket> BRACKETS_2025 = List.of(
            TaxBracket.builder().year(2025).bracketMin(0).bracketMax(14_000_000L).rate(0.06).progressiveDeduction(0L).build(),
            TaxBracket.builder().year(2025).bracketMin(14_000_001L).bracketMax(50_000_000L).rate(0.15).progressiveDeduction(1_260_000L).build(),
            TaxBracket.builder().year(2025).bracketMin(50_000_001L).bracketMax(88_000_000L).rate(0.24).progressiveDeduction(5_760_000L).build(),
            TaxBracket.builder().year(2025).bracketMin(88_000_001L).bracketMax(150_000_000L).rate(0.35).progressiveDeduction(15_440_000L).build(),
            TaxBracket.builder().year(2025).bracketMin(150_000_001L).bracketMax(300_000_000L).rate(0.38).progressiveDeduction(19_940_000L).build(),
            TaxBracket.builder().year(2025).bracketMin(300_000_001L).bracketMax(500_000_000L).rate(0.40).progressiveDeduction(25_940_000L).build(),
            TaxBracket.builder().year(2025).bracketMin(500_000_001L).bracketMax(1_000_000_000L).rate(0.42).progressiveDeduction(35_940_000L).build(),
            TaxBracket.builder().year(2025).bracketMin(1_000_000_001L).bracketMax(9_999_999_999L).rate(0.45).progressiveDeduction(65_940_000L).build()
    );

    @Test
    @DisplayName("종합소득세 계산: 과세표준 1000만원 → 세율 6%, 60만원")
    void incomeTax6Percent() {
        TaxCalculationEngine engine = new TaxCalculationEngine(null, null, null, null);
        long tax = engine.calculateIncomeTaxFromBrackets(10_000_000L, BRACKETS_2025);
        // 10,000,000 * 6% - 0 = 600,000
        assertThat(tax).isEqualTo(600_000L);
    }

    @Test
    @DisplayName("종합소득세 계산: 과세표준 3000만원 → 세율 15%")
    void incomeTax15Percent() {
        TaxCalculationEngine engine = new TaxCalculationEngine(null, null, null, null);
        long tax = engine.calculateIncomeTaxFromBrackets(30_000_000L, BRACKETS_2025);
        // 30,000,000 * 15% - 1,260,000 = 3,240,000
        assertThat(tax).isEqualTo(3_240_000L);
    }

    @Test
    @DisplayName("종합소득세 계산: 과세표준 7000만원 → 세율 24%")
    void incomeTax24Percent() {
        TaxCalculationEngine engine = new TaxCalculationEngine(null, null, null, null);
        long tax = engine.calculateIncomeTaxFromBrackets(70_000_000L, BRACKETS_2025);
        // 70,000,000 * 24% - 5,760,000 = 11,040,000
        assertThat(tax).isEqualTo(11_040_000L);
    }

    @Test
    @DisplayName("종합소득세 계산: 과세표준 0 → 세금 0")
    void incomeTaxZero() {
        TaxCalculationEngine engine = new TaxCalculationEngine(null, null, null, null);
        assertThat(engine.calculateIncomeTaxFromBrackets(0L, BRACKETS_2025)).isEqualTo(0L);
        assertThat(engine.calculateIncomeTaxFromBrackets(-100L, BRACKETS_2025)).isEqualTo(0L);
    }

    @Test
    @DisplayName("절세 예상: 매출 5000만 - 경비 2000만 = 과세표준 3000만")
    void estimationWithExpenses() {
        Long userId = 1L;

        given(bookEntryRepository.safeAggregate(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(new AggregateResult(50_000_000L, 20_000_000L, 0L, 20_000_000L));
        given(bookEntryRepository.sumSalesVat(eq(userId), any(), any())).willReturn(4_545_455L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(userId), any(), any())).willReturn(1_818_182L);
        given(taxCalculationEngine.loadBrackets(anyInt())).willReturn(BRACKETS_2025);
        given(taxCalculationEngine.calculateIncomeTaxFromBrackets(eq(30_000_000L), any())).willReturn(3_240_000L);
        given(taxCalculationEngine.calculateIncomeTaxFromBrackets(eq(50_000_000L), any())).willReturn(6_240_000L);
        given(taxCalculationEngine.findBracketLabel(eq(30_000_000L), any())).willReturn("15%");
        given(taxParameterService.getLocalTaxRate(anyInt())).willReturn(0.10);
        // 기장세액공제 648,000 = min(3,240,000 * 0.20, 1,000,000)
        given(taxCalculationEngine.computeTaxCredits(eq(userId), anyInt(), eq(3_240_000L)))
                .willReturn(Map.of("기장세액공제", 648_000L));
        lenient().when(taxCalculationEngine.computeTaxCredits(eq(userId), anyInt(), eq(6_240_000L)))
                .thenReturn(Map.of("기장세액공제", 1_000_000L));
        given(taxDeadlineRepository.findByYearAndName(anyInt(), anyString())).willReturn(Optional.empty());

        TaxEstimationResponse result = taxEstimationService.estimate(userId, 2026);

        // 산출세액 3,240,000 - 기장세액공제 648,000 = 2,592,000
        long expectedIncomeTax = 3_240_000L - 648_000L;
        assertThat(result.totalIncome()).isEqualTo(50_000_000L);
        assertThat(result.totalExpense()).isEqualTo(20_000_000L);
        assertThat(result.taxableIncome()).isEqualTo(30_000_000L);
        assertThat(result.estimatedIncomeTax()).isEqualTo(expectedIncomeTax);
        assertThat(result.incomeTaxBracket()).isEqualTo("15%");
        assertThat(result.estimatedLocalTax()).isEqualTo((long) Math.floor(expectedIncomeTax * 0.10));
        assertThat(result.estimatedVat()).isEqualTo(4_545_455L - 1_818_182L);
        assertThat(result.taxSavingFromExpenses()).isGreaterThan(0);
    }

    @Test
    @DisplayName("절세 예상: 미확인 장부는 집계에서 제외")
    void unconfirmedEntriesExcluded() {
        Long userId = 1L;

        given(bookEntryRepository.safeAggregate(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(new AggregateResult(0L, 0L, 0L, 0L));
        given(bookEntryRepository.sumSalesVat(eq(userId), any(), any())).willReturn(0L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(userId), any(), any())).willReturn(0L);
        given(taxCalculationEngine.loadBrackets(anyInt())).willReturn(BRACKETS_2025);
        given(taxCalculationEngine.calculateIncomeTaxFromBrackets(anyLong(), any())).willReturn(0L);
        given(taxCalculationEngine.findBracketLabel(anyLong(), any())).willReturn("6%");
        given(taxParameterService.getLocalTaxRate(anyInt())).willReturn(0.10);
        given(taxDeadlineRepository.findByYearAndName(anyInt(), anyString())).willReturn(Optional.empty());

        TaxEstimationResponse result = taxEstimationService.estimate(userId, 2026);

        assertThat(result.totalIncome()).isEqualTo(0L);
        assertThat(result.estimatedIncomeTax()).isEqualTo(0L);
    }

    // ───────────── estimateMonthly ─────────────

    @Test
    @DisplayName("월별 추정: 1기(1-6월) VAT 기간 및 종소세 연환산")
    void estimateMonthly_firstHalf() {
        Long userId = 1L;

        given(bookEntryRepository.sumSalesVat(eq(userId), any(), any())).willReturn(3_000_000L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(userId), any(), any())).willReturn(1_000_000L);
        given(bookEntryRepository.safeAggregate(eq(userId), any(), any()))
                .willReturn(new AggregateResult(15_000_000L, 10_000_000L, 0L, 6_000_000L));
        given(taxCalculationEngine.loadBrackets(anyInt())).willReturn(BRACKETS_2025);
        // 연환산 과세표준: 60M - 24M = 36M → 15% 구간
        long expectedTax = (long) Math.floor(36_000_000L * 0.15 - 1_260_000L); // 4,140,000
        given(taxCalculationEngine.calculateIncomeTaxFromBrackets(eq(36_000_000L), any())).willReturn(expectedTax);
        given(taxParameterService.getLocalTaxRate(anyInt())).willReturn(0.10);
        // 기장세액공제: min(4,140,000 * 0.20, 1,000,000) = 828,000
        given(taxCalculationEngine.computeTaxCredits(eq(userId), anyInt(), eq(expectedTax)))
                .willReturn(Map.of("기장세액공제", Math.min((long)(expectedTax * 0.20), 1_000_000L)));
        given(taxDeadlineRepository.findByYearAndName(anyInt(), anyString())).willReturn(Optional.empty());

        MonthlyTaxEstimationResponse result = taxEstimationService.estimateMonthly(userId, 2026, 3);

        assertThat(result.estimatedVat().estimatedPayable()).isEqualTo(2_000_000L);
        assertThat(result.estimatedVat().period()).contains("1기");
        assertThat(result.estimatedIncomeTax().projectedAnnualIncome()).isEqualTo(60_000_000L);
        assertThat(result.estimatedIncomeTax().projectedAnnualExpense()).isEqualTo(24_000_000L);
        assertThat(result.estimatedIncomeTax().estimatedTax()).isGreaterThan(0);
        assertThat(result.estimatedLocalTax().amount())
                .isEqualTo((long) Math.floor(result.estimatedIncomeTax().estimatedTax() * 0.1));
    }

    @Test
    @DisplayName("월별 추정: 2기(7-12월) VAT 기간 정확")
    void estimateMonthly_secondHalf() {
        Long userId = 1L;

        given(bookEntryRepository.sumSalesVat(eq(userId), any(), any())).willReturn(0L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(userId), any(), any())).willReturn(0L);
        given(bookEntryRepository.safeAggregate(eq(userId), any(), any()))
                .willReturn(new AggregateResult(0L, 0L, 0L, 0L));
        given(taxCalculationEngine.loadBrackets(anyInt())).willReturn(BRACKETS_2025);
        given(taxCalculationEngine.calculateIncomeTaxFromBrackets(anyLong(), any())).willReturn(0L);
        given(taxParameterService.getLocalTaxRate(anyInt())).willReturn(0.10);
        given(taxDeadlineRepository.findByYearAndName(anyInt(), anyString())).willReturn(Optional.empty());

        MonthlyTaxEstimationResponse result = taxEstimationService.estimateMonthly(userId, 2026, 9);

        assertThat(result.estimatedVat().period()).contains("2기");
        assertThat(result.estimatedIncomeTax().estimatedTax()).isZero();
        assertThat(result.totalEstimatedTax()).isZero();
    }

    @Test
    @DisplayName("월별 추정: currentExpense(DTO)는 businessExpense(agg[3])를 사용")
    void estimateMonthly_usesBusinessExpense_notTotalExpense() {
        Long userId = 1L;

        given(bookEntryRepository.sumSalesVat(eq(userId), any(), any())).willReturn(0L);
        given(bookEntryRepository.sumDeductiblePurchaseVat(eq(userId), any(), any())).willReturn(0L);
        given(bookEntryRepository.safeAggregate(eq(userId), any(), any()))
                .willReturn(new AggregateResult(30_000_000L, 20_000_000L, 0L, 8_000_000L));
        given(taxCalculationEngine.loadBrackets(anyInt())).willReturn(BRACKETS_2025);
        given(taxCalculationEngine.calculateIncomeTaxFromBrackets(anyLong(), any())).willReturn(0L);
        given(taxParameterService.getLocalTaxRate(anyInt())).willReturn(0.10);
        given(taxDeadlineRepository.findByYearAndName(anyInt(), anyString())).willReturn(Optional.empty());

        MonthlyTaxEstimationResponse result = taxEstimationService.estimateMonthly(userId, 2026, 6);

        assertThat(result.estimatedIncomeTax().currentExpense()).isEqualTo(8_000_000L);
        assertThat(result.estimatedIncomeTax().projectedAnnualExpense()).isEqualTo(16_000_000L);
    }

    private BookEntry createEntry(EntryType type, long income, long expense, long asset, long vat,
                                   boolean business, boolean confirmed) {
        BookEntry entry = BookEntry.builder()
                .userId(1L)
                .entryDate(LocalDate.of(2026, 3, 1))
                .entryType(type)
                .incomeAmount(income)
                .expenseAmount(expense)
                .fixedAssetAmount(asset)
                .vatAmount(vat)
                .build();
        if (business) entry.markAsBusiness();
        if (confirmed) entry.confirm();
        return entry;
    }
}
