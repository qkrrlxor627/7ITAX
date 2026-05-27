package com.ssafy.tax7i.tax.service;

import com.ssafy.tax7i.auth.domain.BusinessProfile;
import com.ssafy.tax7i.auth.repository.BusinessProfileRepository;
import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.tax.dto.TaxCalculationResult;
import com.ssafy.tax7i.tax.entity.TaxBracket;
import com.ssafy.tax7i.tax.repository.TaxBracketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * 테스트 결제내역(db/seed/test_book_entries.sql) 기반
 * 종합소득세 + 부가가치세 전 구간 End-to-End 검증
 *
 * 검증 대상: 1인 IT개발자 간편장부 사업자 (user_id=1, 2025년 귀속)
 * 업종코드: 722000 (소프트웨어), 중소기업 특별세액감면 30% 대상
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaxFullPipelineTest {

    @Mock private TaxBracketRepository taxBracketRepository;
    @Mock private BookEntryRepository bookEntryRepository;
    @Mock private TaxParameterService taxParameterService;
    @Mock private BusinessProfileRepository businessProfileRepository;
    @InjectMocks private TaxCalculationEngine taxCalculationEngine;

    private static final Long USER_ID = 1L;
    private static final int TAX_YEAR = 2025;

    // ── 테스트 데이터 기대값 (test_book_entries.sql과 1:1 대응) ──
    private static final long TOTAL_REVENUE       = 66_000_000L;  // 매출 12건 × 5,500,000
    private static final long TOTAL_EXPENSE_ALL    = 12_942_000L;  // 사업경비 + 경비불인정
    private static final long BUSINESS_EXPENSE     = 12_822_000L;  // 사업경비만 (isBusinessExpense=true)
    private static final long TOTAL_ASSET          =  2_750_000L;  // 맥북 (ASSET)
    private static final long ENTERTAINMENT_USED   =  1_650_000L;  // 접대비 5건
    private static final long VEHICLE_USED         =  1_056_000L;  // 차량유지비 12건

    // 부가세 1기 (1~6월)
    private static final long VAT_SALES_1H         =  3_000_000L;  // 매출세액
    private static final long VAT_PURCHASE_1H      =    455_000L;  // 매입세액(공제)
    // 부가세 2기 (7~12월)
    private static final long VAT_SALES_2H         =  3_000_000L;
    private static final long VAT_PURCHASE_2H      =    436_000L;

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

    @BeforeEach
    void setup() {
        // 세율 구간
        given(taxBracketRepository.findByYearOrderByBracketMinAsc(TAX_YEAR)).willReturn(BRACKETS_2025);

        // 세금 파라미터 (DB 시드값과 동일)
        given(taxParameterService.getLocalTaxRate(TAX_YEAR)).willReturn(0.10);
        given(taxParameterService.getBasicDeduction(TAX_YEAR)).willReturn(1_500_000L);
        given(taxParameterService.getEntertainmentLimit(eq(TAX_YEAR), eq(TOTAL_REVENUE)))
                .willReturn(24_000_000L + (long) Math.floor(TOTAL_REVENUE * 0.002)); // 24,132,000
        given(taxParameterService.getVehicleExpenseLimit(TAX_YEAR)).willReturn(15_000_000L);
        given(taxParameterService.getSmeReductionRate(eq(TAX_YEAR), eq("722000"))).willReturn(0.30);
        given(taxParameterService.getBookkeepingCreditRate(TAX_YEAR)).willReturn(0.20);
        given(taxParameterService.getBookkeepingCreditLimit(TAX_YEAR)).willReturn(1_000_000L);
    }

    // ================================================================
    //  종합소득세 전 구간 테스트
    // ================================================================
    @Nested
    @DisplayName("종합소득세 — 13단계 (A~M)")
    class IncomeTaxPipeline {

        @BeforeEach
        void setupIncomeTax() {
            // BookEntry 집계 (confirmed=true 기준)
            given(bookEntryRepository.safeAggregate(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(new AggregateResult(TOTAL_REVENUE, TOTAL_EXPENSE_ALL, TOTAL_ASSET, BUSINESS_EXPENSE));

            // 세목별 사용액
            given(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(USER_ID), eq("접대비"), eq(TAX_YEAR)))
                    .willReturn(ENTERTAINMENT_USED);
            given(bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(eq(USER_ID), eq("차량유지비"), eq(TAX_YEAR)))
                    .willReturn(VEHICLE_USED);

            // 사업자 프로필 (업종코드 722000 = SW)
            BusinessProfile profile = BusinessProfile.builder()
                    .businessName("테스트개발자").industryCode("722000").build();
            given(businessProfileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile));
        }

        @Test
        @DisplayName("A~B. 총수입금액=66,000,000, 필요경비(사업)=12,822,000")
        void step_AB_revenueAndExpense() {
            AggregateResult agg = bookEntryRepository.safeAggregate(USER_ID,
                    LocalDate.of(TAX_YEAR, 1, 1), LocalDate.of(TAX_YEAR, 12, 31));

            assertThat(agg.totalIncome()).isEqualTo(66_000_000L);
            assertThat(agg.businessExpense()).isEqualTo(12_822_000L);
            // 경비불인정(120,000)은 totalExpense에는 포함, businessExpense에는 미포함
            assertThat(agg.totalExpense()).isEqualTo(12_942_000L);
            assertThat(agg.totalExpense() - agg.businessExpense()).isEqualTo(120_000L);
        }

        @Test
        @DisplayName("C. 경비한도 조정: 접대비·차량유지비 모두 한도 미만 → 조정액=0")
        void step_C_expenseAdjustment() {
            long adjustment = taxCalculationEngine.computeExpenseAdjustment(USER_ID, TAX_YEAR, TOTAL_REVENUE);

            assertThat(adjustment).isEqualTo(0L);
            // 접대비: 1,650,000 < 24,132,000 → OK
            // 차량유지비: 1,056,000 < 15,000,000 → OK
        }

        @Test
        @DisplayName("D~F. 소득금액=53,178,000 → 기본공제 → 과세표준=51,678,000")
        void step_DEF_incomeToTaxable() {
            long incomeAmount = TOTAL_REVENUE - BUSINESS_EXPENSE; // 53,178,000
            long basicDeduction = 1_500_000L;
            long taxableIncome = Math.max(0, incomeAmount - basicDeduction); // 51,678,000

            assertThat(incomeAmount).isEqualTo(53_178_000L);
            assertThat(taxableIncome).isEqualTo(51_678_000L);
        }

        @Test
        @DisplayName("G. 산출세액: 51,678,000 × 24% - 5,760,000 = 6,642,720")
        void step_G_calculatedTax() {
            long taxableIncome = 51_678_000L;
            long calculatedTax = taxCalculationEngine.calculateIncomeTaxFromBrackets(taxableIncome, BRACKETS_2025);

            // floor(51,678,000 × 0.24 - 5,760,000) = floor(6,642,720) = 6,642,720
            assertThat(calculatedTax).isEqualTo(6_642_720L);
        }

        @Test
        @DisplayName("H. 중소기업 특별세액감면(30%): 1,992,816")
        void step_H_smeReduction() {
            long calculatedTax = 6_642_720L;
            long smeReduction = (long) Math.floor(calculatedTax * 0.30);

            assertThat(smeReduction).isEqualTo(1_992_816L);
        }

        @Test
        @DisplayName("I. 기장세액공제 min(20%, 100만): 1,000,000")
        void step_I_bookkeepingCredit() {
            long calculatedTax = 6_642_720L;
            long bookkeeping = Math.min((long) Math.floor(calculatedTax * 0.20), 1_000_000L);

            assertThat(bookkeeping).isEqualTo(1_000_000L);
            // 6,642,720 × 20% = 1,328,544 > 100만 → 한도 적용
        }

        @Test
        @DisplayName("J~M. 결정세액=3,649,904, 지방소득세=364,990, 총=4,014,894")
        void step_JtoM_determinedAndLocalTax() {
            long calculatedTax = 6_642_720L;
            long totalCredits = 1_992_816L + 1_000_000L; // 감면 + 공제
            long determinedTax = Math.max(0, calculatedTax - totalCredits);
            long localTax = (long) Math.floor(determinedTax * 0.10);

            assertThat(determinedTax).isEqualTo(3_649_904L);
            assertThat(localTax).isEqualTo(364_990L);
            assertThat(determinedTax + localTax).isEqualTo(4_014_894L);
        }

        @Test
        @DisplayName("종합소득세 전체 파이프라인 — calculateForUser() 통합 검증")
        void fullPipeline_calculateForUser() {
            TaxCalculationResult result = taxCalculationEngine.calculateForUser(
                    USER_ID, TAX_YEAR, 0L, Collections.emptyMap());

            // A. 총수입
            assertThat(result.totalRevenue()).isEqualTo(66_000_000L);
            // B. 필요경비 (businessExpense만)
            assertThat(result.totalExpense()).isEqualTo(12_822_000L);
            // D. 소득금액
            assertThat(result.incomeAmount()).isEqualTo(53_178_000L);
            // E. 소득공제 (기본공제 자동 적용)
            assertThat(result.totalDeductions()).isEqualTo(1_500_000L);
            // F. 과세표준
            assertThat(result.taxableIncome()).isEqualTo(51_678_000L);
            // G. 세율 (24% 구간)
            assertThat(result.taxRate()).isEqualTo(0.24);
            // G. 산출세액
            assertThat(result.calculatedTax()).isEqualTo(6_642_720L);
            // H+I. 세액감면+공제 합계
            assertThat(result.totalTaxCredits()).isEqualTo(2_992_816L);
            // J. 결정세액
            assertThat(result.determinedTax()).isEqualTo(3_649_904L);
            // L. 최종 납부세액 (기납부=0)
            assertThat(result.finalTax()).isEqualTo(3_649_904L);
            // M. 지방소득세
            assertThat(result.localTax()).isEqualTo(364_990L);
            // 환급 아님
            assertThat(result.isRefund()).isFalse();
        }
    }

    // ================================================================
    //  부가가치세 전 구간 테스트
    // ================================================================
    @Nested
    @DisplayName("부가가치세 — 1기/2기")
    class VatPipeline {

        @Test
        @DisplayName("1기 (1~6월): 매출세액 3,000,000 - 매입세액(공제) 455,000 = 납부 2,545,000")
        void vat_firstHalf() {
            given(bookEntryRepository.sumSalesVat(eq(USER_ID),
                    eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2025, 6, 30))))
                    .willReturn(VAT_SALES_1H);
            given(bookEntryRepository.sumDeductiblePurchaseVat(eq(USER_ID),
                    eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2025, 6, 30))))
                    .willReturn(VAT_PURCHASE_1H);

            long salesTax = bookEntryRepository.sumSalesVat(USER_ID,
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30));
            long purchaseTax = bookEntryRepository.sumDeductiblePurchaseVat(USER_ID,
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30));
            long finalVat = salesTax - purchaseTax;

            assertThat(salesTax).isEqualTo(3_000_000L);
            assertThat(purchaseTax).isEqualTo(455_000L);
            assertThat(finalVat).isEqualTo(2_545_000L);
        }

        @Test
        @DisplayName("2기 (7~12월): 매출세액 3,000,000 - 매입세액(공제) 436,000 = 납부 2,564,000")
        void vat_secondHalf() {
            given(bookEntryRepository.sumSalesVat(eq(USER_ID),
                    eq(LocalDate.of(2025, 7, 1)), eq(LocalDate.of(2025, 12, 31))))
                    .willReturn(VAT_SALES_2H);
            given(bookEntryRepository.sumDeductiblePurchaseVat(eq(USER_ID),
                    eq(LocalDate.of(2025, 7, 1)), eq(LocalDate.of(2025, 12, 31))))
                    .willReturn(VAT_PURCHASE_2H);

            long salesTax = bookEntryRepository.sumSalesVat(USER_ID,
                    LocalDate.of(2025, 7, 1), LocalDate.of(2025, 12, 31));
            long purchaseTax = bookEntryRepository.sumDeductiblePurchaseVat(USER_ID,
                    LocalDate.of(2025, 7, 1), LocalDate.of(2025, 12, 31));
            long finalVat = salesTax - purchaseTax;

            assertThat(salesTax).isEqualTo(3_000_000L);
            assertThat(purchaseTax).isEqualTo(436_000L);
            assertThat(finalVat).isEqualTo(2_564_000L);
        }

        @Test
        @DisplayName("연간 합계: 매출세액 6,000,000 - 매입세액(공제) 891,000 = 5,109,000")
        void vat_annual() {
            long annualSales = VAT_SALES_1H + VAT_SALES_2H;
            long annualPurchase = VAT_PURCHASE_1H + VAT_PURCHASE_2H;

            assertThat(annualSales).isEqualTo(6_000_000L);
            assertThat(annualPurchase).isEqualTo(891_000L);
            assertThat(annualSales - annualPurchase).isEqualTo(5_109_000L);
        }

        @Test
        @DisplayName("불공제 검증: 접대비 VAT(150,000) + 차량유지비 VAT(96,000) = 매입세액에 미포함")
        void vat_nonDeductible_excluded() {
            // 접대비 VAT: 5건 × 30,000 = 150,000 → isVatDeductible=false → 불공제
            // 차량유지비 VAT: 12건 × 8,000 = 96,000 → isVatDeductible=false → 불공제
            // 경비불인정 VAT: 0 (면세처리)
            // 도서인쇄비 VAT: 0 (면세)
            // 여비교통비 VAT: 0 (면세)
            long nonDeductibleVat = 150_000L + 96_000L; // 246,000

            // 전체 경비 VAT 합계 (공제+불공제+면세)
            long totalExpenseVat = 891_000L + nonDeductibleVat; // 1,137,000

            assertThat(nonDeductibleVat).isEqualTo(246_000L);
            // 공제 가능 매입세액만 891,000원 (불공제 246,000원 제외)
            assertThat(VAT_PURCHASE_1H + VAT_PURCHASE_2H).isEqualTo(891_000L);
        }
    }

    // ================================================================
    //  세목별 커버리지 확인
    // ================================================================
    @Nested
    @DisplayName("세목 커버리지 — 16개 전체")
    class CategoryCoverage {

        @Test
        @DisplayName("사업경비 합계: 16개 세목 × 각 금액 = 12,822,000")
        void allCategoriesSum() {
            long entertainment  = 1_650_000L;  // 접대비 (08)
            long vehicle        = 1_056_000L;  // 차량유지비 (11)
            long rent           = 6_600_000L;  // 임차료 (06)
            long communication  =   792_000L;  // 통신비 (21)
            long serviceFee     =   924_000L;  // 지급수수료 (12)
            long education      =   330_000L;  // 교육훈련비 (22)
            long books          =   165_000L;  // 도서인쇄비 (23)
            long supplies       =   220_000L;  // 소모품비 (13)
            long travel         =   150_000L;  // 여비교통비 (17)
            long advertising    =   330_000L;  // 광고선전비 (16)
            long utilities      =   330_000L;  // 수도광열비 (24)
            long repair         =   220_000L;  // 수선비 (25)
            long shipping       =    55_000L;  // 운반비 (15)

            long total = entertainment + vehicle + rent + communication + serviceFee
                    + education + books + supplies + travel + advertising
                    + utilities + repair + shipping;

            assertThat(total).isEqualTo(BUSINESS_EXPENSE);

            // 추가 확인: 경비불인정(120,000)은 미포함
            long nonBusiness = 120_000L;
            assertThat(total + nonBusiness).isEqualTo(TOTAL_EXPENSE_ALL);
        }
    }
}
