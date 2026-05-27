package com.ssafy.tax7i.taxestimation.service;

import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.entity.TaxBracket;
import com.ssafy.tax7i.tax.service.TaxCalculationEngine;
import com.ssafy.tax7i.tax.service.TaxParameterService;
import com.ssafy.tax7i.taxcalendar.repository.TaxDeadlineRepository;
import com.ssafy.tax7i.taxestimation.dto.MonthlyTaxEstimationResponse;
import com.ssafy.tax7i.taxestimation.dto.TaxEstimationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxEstimationService {

    private final BookEntryRepository bookEntryRepository;
    private final TaxCalculationEngine taxCalculationEngine;
    private final TaxParameterService taxParameterService;
    private final TaxDeadlineRepository taxDeadlineRepository;

    public TaxEstimationResponse estimate(Long userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        // DB 집계 쿼리로 전체 데이터 로드 없이 합산
        AggregateResult agg = bookEntryRepository.safeAggregate(userId, start, end);
        long totalIncome = agg.totalIncome();
        long totalExpense = agg.totalExpense();
        long totalAssetPurchase = agg.totalAsset();
        long deductibleExpenses = agg.businessExpense();

        // 부가세: 매출세액 - 매입세액
        Long salesVatRaw = bookEntryRepository.sumSalesVat(userId, start, end);
        Long purchaseVatRaw = bookEntryRepository.sumDeductiblePurchaseVat(userId, start, end);
        long salesVat = salesVatRaw != null ? salesVatRaw : 0L;
        long purchaseVat = purchaseVatRaw != null ? purchaseVatRaw : 0L;
        long estimatedVat = Math.max(0, salesVat - purchaseVat);

        // 소득세법 §33·§35·§33의2: 세목별 한도 초과 경비 조정
        long expenseAdjustment = taxCalculationEngine.computeExpenseAdjustment(userId, year, totalIncome);
        long adjustedExpenses = Math.max(0, deductibleExpenses - expenseAdjustment);

        // 종합소득세: 과세표준 = 수입 - 필요경비
        long taxableIncome = Math.max(0, totalIncome - adjustedExpenses);
        List<TaxBracket> brackets = taxCalculationEngine.loadBrackets(year);
        long calculatedTax = taxCalculationEngine.calculateIncomeTaxFromBrackets(taxableIncome, brackets);
        String bracket = taxCalculationEngine.findBracketLabel(taxableIncome, brackets);

        // 세액감면/공제 적용 (조특법 §7, 소득세법 §56조의2)
        long totalCredits = computeEstimationCredits(userId, year, calculatedTax);
        long incomeTax = Math.max(0, calculatedTax - totalCredits);

        // 지방소득세: 종합소득세의 지방세율 (DB)
        double localTaxRate = taxParameterService.getLocalTaxRate(year);
        long localTax = (long) Math.floor(incomeTax * localTaxRate);

        // 절세 효과: 경비 없었을 때 세금 - 현재 세금
        long taxWithoutExpenses = taxCalculationEngine.calculateIncomeTaxFromBrackets(totalIncome, brackets);
        long taxSaving = taxWithoutExpenses - incomeTax;

        // 부가세 반기별 (부가가치세법 §5, §49)
        TaxEstimationResponse.VatByPeriod vatByPeriod = buildVatByPeriod(userId, year);

        // 연간 총 예상 세금
        long totalAnnualTax = vatByPeriod.totalVat() + incomeTax + localTax;

        // 세율 구간 상세 (소득세법 §55)
        TaxEstimationResponse.BracketDetail bracketDetail = buildBracketDetail(taxableIncome, brackets);

        return new TaxEstimationResponse(
                year,
                totalIncome, totalExpense, totalAssetPurchase, taxableIncome,
                salesVat, purchaseVat, estimatedVat,
                incomeTax, bracket, localTax,
                deductibleExpenses, taxSaving,
                vatByPeriod, totalAnnualTax, bracketDetail
        );
    }

    /**
     * 월별 세금 추정
     * - 부가세: 해당 과세기간(1기/2기) 누적 매출세액-매입세액 (부가가치세법 §49)
     * - 종소세: 현재까지 수입/비용을 연환산 (소득세법 §70)
     * - 지방세: 종소세 결정세액 × 지방세율 (지방세법 §92, §95)
     */
    public MonthlyTaxEstimationResponse estimateMonthly(Long userId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "월은 1~12 범위여야 합니다: " + month);
        }

        // 부가세: 해당 과세기간 누적 (1기: 1~6월, 2기: 7~12월)
        boolean isFirstHalf = month <= 6;
        LocalDate vatStart = isFirstHalf ? LocalDate.of(year, 1, 1) : LocalDate.of(year, 7, 1);
        LocalDate vatEnd = LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth());
        String vatPeriod = isFirstHalf ? "1기 (1~6월)" : "2기 (7~12월)";
        String vatDueDate = resolveVatDueDate(year, isFirstHalf);

        Long salesVatRaw = bookEntryRepository.sumSalesVat(userId, vatStart, vatEnd);
        Long purchaseVatRaw = bookEntryRepository.sumDeductiblePurchaseVat(userId, vatStart, vatEnd);
        long salesTax = salesVatRaw != null ? salesVatRaw : 0L;
        long purchaseTax = purchaseVatRaw != null ? purchaseVatRaw : 0L;
        long vatPayable = Math.max(0, salesTax - purchaseTax);

        var vatEstimation = new MonthlyTaxEstimationResponse.VatEstimation(
                vatPeriod, salesTax, purchaseTax, vatPayable, vatDueDate);

        // 종소세: 현재까지 수입/비용을 연환산
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate monthEnd = LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth());

        AggregateResult monthlyAgg = bookEntryRepository.safeAggregate(userId, yearStart, monthEnd);
        long currentIncome = monthlyAgg.totalIncome();
        long currentBusinessExpense = monthlyAgg.businessExpense();

        // 연환산 먼저 적용 (소득세법 §70) — 경비한도는 연간 기준이므로 연환산 후 적용해야 정확
        long projectedIncome = month > 0 ? currentIncome * 12 / month : 0;
        long projectedBusinessExpense = month > 0 ? currentBusinessExpense * 12 / month : 0;

        // 소득세법 §33·§35·§33의2: 연환산 수입 기준으로 경비한도 적용
        long projectedAdjustment = taxCalculationEngine.computeExpenseAdjustment(userId, year, projectedIncome);
        projectedAdjustment = month > 0 ? projectedAdjustment * 12 / month : 0;
        projectedBusinessExpense = Math.max(0, projectedBusinessExpense - projectedAdjustment);

        long projectedTaxable = Math.max(0, projectedIncome - projectedBusinessExpense);
        List<TaxBracket> brackets = taxCalculationEngine.loadBrackets(year);
        long projectedCalculatedTax = taxCalculationEngine.calculateIncomeTaxFromBrackets(projectedTaxable, brackets);

        // 세액감면/공제 적용
        long monthlyCredits = computeEstimationCredits(userId, year, projectedCalculatedTax);
        long incomeTax = Math.max(0, projectedCalculatedTax - monthlyCredits);
        String incomeTaxDueDate = resolveIncomeTaxDueDate(year);

        // businessExpense → DTO의 currentExpense / projectedAnnualExpense 필드에 매핑
        var incomeTaxEstimation = new MonthlyTaxEstimationResponse.IncomeTaxEstimation(
                currentIncome, currentBusinessExpense, projectedIncome, projectedBusinessExpense,
                incomeTax, incomeTaxDueDate);

        // 지방세: 종소세 결정세액 × 지방세율 (지방세법 §92)
        double localTaxRate = taxParameterService.getLocalTaxRate(year);
        long localTax = (long) Math.floor(incomeTax * localTaxRate);
        var localTaxEstimation = new MonthlyTaxEstimationResponse.LocalTaxEstimation(
                localTax, incomeTaxDueDate);

        long totalTax = vatPayable + incomeTax + localTax;

        return new MonthlyTaxEstimationResponse(
                year, month, vatEstimation, incomeTaxEstimation, localTaxEstimation, totalTax);
    }

    /** 부가세 반기별 계산 — 부가가치세법 §5(과세기간), §49(확정신고), §39①1(접대비 불공제) */
    private TaxEstimationResponse.VatByPeriod buildVatByPeriod(Long userId, int year) {
        var p1 = buildVatPeriodDetail(userId, year, 1, 6,
                "1기 (1~6월)", resolveVatDueDate(year, true));
        var p2 = buildVatPeriodDetail(userId, year, 7, 12,
                "2기 (7~12월)", resolveVatDueDate(year, false));
        return new TaxEstimationResponse.VatByPeriod(p1, p2, p1.estimatedPayable() + p2.estimatedPayable());
    }

    private TaxEstimationResponse.VatPeriodDetail buildVatPeriodDetail(
            Long userId, int year, int startMonth, int endMonth, String label, String dueDate) {
        LocalDate s = LocalDate.of(year, startMonth, 1);
        LocalDate e = LocalDate.of(year, endMonth, LocalDate.of(year, endMonth, 1).lengthOfMonth());
        Long svRaw = bookEntryRepository.sumSalesVat(userId, s, e);
        Long pvRaw = bookEntryRepository.sumDeductiblePurchaseVat(userId, s, e);
        long sv = svRaw != null ? svRaw : 0L;
        long pv = pvRaw != null ? pvRaw : 0L;
        return new TaxEstimationResponse.VatPeriodDetail(label, sv, pv, Math.max(0, sv - pv), dueDate);
    }

    /** 세율 구간 상세 — 소득세법 §55 */
    private TaxEstimationResponse.BracketDetail buildBracketDetail(long taxableIncome, List<TaxBracket> brackets) {
        long prevMax = 0;
        for (int i = 0; i < brackets.size(); i++) {
            TaxBracket b = brackets.get(i);
            if (taxableIncome >= b.getBracketMin() && taxableIncome <= b.getBracketMax()) {
                long bracketMax = b.getBracketMax();
                String currentRate = Math.round(b.getRate() * 100) + "%";
                Long toNext = (bracketMax >= 9_999_999_999L) ? null : bracketMax - taxableIncome;
                String nextRate = (i + 1 < brackets.size() && bracketMax < 9_999_999_999L)
                        ? Math.round(brackets.get(i + 1).getRate() * 100) + "%"
                        : null;
                return new TaxEstimationResponse.BracketDetail(
                        currentRate, prevMax, bracketMax, taxableIncome, toNext, nextRate);
            }
            prevMax = b.getBracketMax();
        }
        // 최고 구간
        if (!brackets.isEmpty()) {
            TaxBracket last = brackets.get(brackets.size() - 1);
            return new TaxEstimationResponse.BracketDetail(
                    Math.round(last.getRate() * 100) + "%",
                    last.getBracketMin(), last.getBracketMax(), taxableIncome, null, null);
        }
        return new TaxEstimationResponse.BracketDetail("0%", 0, 0, taxableIncome, null, null);
    }

    /** 종합소득세 신고기한 조회 (DB 우선, 폴백: 기본값) */
    private String resolveIncomeTaxDueDate(int year) {
        int deadlineYear = year + 1;
        return taxDeadlineRepository.findByYearAndName(deadlineYear, "종합소득세 신고")
                .or(() -> taxDeadlineRepository.findByYearAndName(year, "종합소득세 신고"))
                .map(d -> deadlineYear + "-" + String.format("%02d-%02d", d.getDeadlineMonth(), d.getDeadlineDay()))
                .orElse(deadlineYear + "-05-31");
    }

    /** 부가세 확정신고기한 조회 (DB 우선, 폴백: 기본값) */
    private String resolveVatDueDate(int year, boolean isFirstHalf) {
        if (isFirstHalf) {
            String name = "부가가치세 확정신고 (1기)";
            return taxDeadlineRepository.findByYearAndName(year, name)
                    .map(d -> year + "-" + String.format("%02d-%02d", d.getDeadlineMonth(), d.getDeadlineDay()))
                    .orElse(year + "-07-25");
        } else {
            String name = "부가가치세 확정신고 (2기)";
            int nextYear = year + 1;
            return taxDeadlineRepository.findByYearAndName(nextYear, name)
                    .or(() -> taxDeadlineRepository.findByYearAndName(year, name))
                    .map(d -> nextYear + "-" + String.format("%02d-%02d", d.getDeadlineMonth(), d.getDeadlineDay()))
                    .orElse(nextYear + "-01-25");
        }
    }

    /** 세액감면/공제 합산 — 중소기업 특별세액감면(조특법 §7) + 기장세액공제(소득세법 §56조의2) */
    private long computeEstimationCredits(Long userId, int year, long calculatedTax) {
        return taxCalculationEngine.computeTaxCredits(userId, year, calculatedTax)
                .values().stream().mapToLong(Long::longValue).sum();
    }
}
