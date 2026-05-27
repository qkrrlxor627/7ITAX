package com.ssafy.tax7i.tax.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.auth.repository.BusinessProfileRepository;
import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.classification.entity.TaxCategory;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.tax.dto.TaxCalculationResult;
import com.ssafy.tax7i.tax.dto.TaxSavingRecommendation;
import com.ssafy.tax7i.tax.dto.TaxSavingResponse;
import com.ssafy.tax7i.tax.dto.TaxSavingSummaryResponse;
import com.ssafy.tax7i.tax.dto.TaxSavingSummaryResponse.LimitTracking;
import com.ssafy.tax7i.tax.dto.TaxSavingSummaryResponse.TaxReductions;
import com.ssafy.tax7i.tax.repository.TaxReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxSavingService {

    /** 노란우산공제 — 소득공제 항목이므로 TaxCategory 아닌 별도 상수 */
    private static final String NORAN_CATEGORY = "노란우산공제";

    private final BookEntryRepository bookEntryRepository;
    private final TaxReturnRepository taxReturnRepository;
    private final TaxCalculationEngine taxCalculationEngine;
    private final TaxParameterService taxParameterService;
    private final BusinessProfileRepository businessProfileRepository;
    private final ObjectMapper objectMapper;

    public TaxSavingResponse getRecommendations(Long userId, int taxYear) {
        // 1. Aggregate book entries for the year
        LocalDate start = LocalDate.of(taxYear, 1, 1);
        LocalDate end = LocalDate.of(taxYear, 12, 31);
        AggregateResult agg = bookEntryRepository.safeAggregate(userId, start, end);
        long totalRevenue = agg.totalIncome();
        long totalExpense = agg.businessExpense();
        long businessIncome = totalRevenue - totalExpense;

        // 2. Base deductions (DB 조회)
        Map<String, Long> baseDeductions = new HashMap<>();
        baseDeductions.put("기본공제_본인", taxParameterService.getBasicDeduction(taxYear));

        // 3. Calculate current tax
        TaxCalculationResult currentTax = taxCalculationEngine.calculate(
                taxYear, totalRevenue, totalExpense, 0, baseDeductions);

        List<TaxSavingRecommendation> recommendations = new ArrayList<>();
        double localTaxRate = taxParameterService.getLocalTaxRate(taxYear);

        // 4. 노란우산공제 (DB 조회) — 사업소득금액 기준
        long noranLimit = taxParameterService.getNoranLimit(taxYear, businessIncome);
        Map<String, Long> withNoran = new HashMap<>(baseDeductions);
        withNoran.put(NORAN_CATEGORY, noranLimit);
        TaxCalculationResult withNoranTax = taxCalculationEngine.calculate(
                taxYear, totalRevenue, totalExpense, 0, withNoran);
        long noranSaving = currentTax.determinedTax() - withNoranTax.determinedTax();
        noranSaving += (long) (noranSaving * localTaxRate); // 지방세 포함 (지방세법 §92)
        long noranPaid = getNoranPaidFromTaxReturn(userId, taxYear);
        recommendations.add(TaxSavingRecommendation.withUsage(
                "DEDUCTION", "노란우산공제",
                "소기업·소상공인 공제부금. 가입 시 연 최대 " + (noranLimit / 10000) + "만원 소득공제.",
                noranLimit, noranSaving, noranPaid > 0, noranPaid));

        // 5. 연금저축 (DB 조회) — 종합소득금액 기준 (소득세법 §59조의3)
        double creditRate = taxParameterService.getPensionCreditRate(taxYear, businessIncome);
        long pensionLimit = taxParameterService.getPensionLimit(taxYear);
        long pensionSaving = (long) Math.floor(pensionLimit * creditRate);
        pensionSaving += (long) (pensionSaving * localTaxRate); // 지방세 포함 (지방세법 §92)
        recommendations.add(TaxSavingRecommendation.withUsage(
                "CREDIT", "연금저축",
                "연 " + (pensionLimit / 10000) + "만원 한도 세액공제 " + (creditRate * 100) + "%",
                pensionLimit, pensionSaving, false, 0));

        // 6. 접대비 한도 여유 (소득세법 §35, 시행령 §79)
        Long entertainmentUsedRaw = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                userId, TaxCategory.ENTERTAINMENT.getName(), taxYear);
        long entertainmentUsed = entertainmentUsedRaw != null ? entertainmentUsedRaw : 0L;
        long entertainmentLimit = taxParameterService.getEntertainmentLimit(taxYear, totalRevenue);
        if (entertainmentUsed < entertainmentLimit) {
            long entertainRemaining = entertainmentLimit - entertainmentUsed;
            long entertainSaving = (long) Math.floor(entertainRemaining * currentTax.taxRate());
            entertainSaving += (long) (entertainSaving * localTaxRate); // 지방세 포함 (지방세법 §92)
            recommendations.add(TaxSavingRecommendation.withUsage(
                    "EXPENSE", "접대비 한도 여유",
                    "올해 접대비 추가 사용 가능. 한도 = 2,400만원 + 수입×0.2% (소득세법 §35, 시행령 §78)",
                    entertainmentLimit, entertainSaving, false, entertainmentUsed));
        }

        // 6-1. 차량유지비 한도 여유 (소득세법 §33의2, 시행령 §78의3)
        Long vehicleUsedRaw = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                userId, TaxCategory.VEHICLE.getName(), taxYear);
        long vehicleUsed = vehicleUsedRaw != null ? vehicleUsedRaw : 0L;
        long vehicleLimit = taxParameterService.getVehicleExpenseLimit(taxYear);
        if (vehicleUsed > 0 && vehicleUsed < vehicleLimit) {
            long vehicleRemaining = vehicleLimit - vehicleUsed;
            long vehicleSaving = (long) Math.floor(vehicleRemaining * currentTax.taxRate());
            vehicleSaving += (long) (vehicleSaving * localTaxRate);
            recommendations.add(TaxSavingRecommendation.withUsage(
                    "EXPENSE", "차량유지비 한도 여유",
                    "업무용 차량비 추가 인정 가능 (연 1,500만원 한도). 업무전용보험 가입 시 전액 인정 (소득세법 §33의2)",
                    vehicleLimit, vehicleSaving, false, vehicleUsed));
        }

        // 7. 사업용카드 매입세액 공제 (부가가치세법 §46, 불공제: §39①1 접대비)
        Long cardBizVatRaw = bookEntryRepository.sumDeductiblePurchaseVat(userId, start, end);
        long cardBizVat = cardBizVatRaw != null ? cardBizVatRaw : 0L;
        if (cardBizVat > 0) {
            recommendations.add(TaxSavingRecommendation.withUsage(
                    "VAT_CREDIT",
                    "사업용카드 매입세액 공제",
                    "사업 관련 카드 결제 시 부가세 환급 (부가가치세법 §46). 접대비는 불공제 (§39①1)",
                    0, cardBizVat, true, cardBizVat));
        }

        // 8. 교육훈련비 경비 활용 (소득세법 §19 필요경비)
        //    ※ 조특법 §104의18(중소기업 교육훈련비 세액공제)은 근로자 대상 → 1인 사업자 본인 미적용
        Long eduUsedRaw = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                userId, TaxCategory.EDUCATION.getName(), taxYear);
        Long booksUsedRaw = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                userId, TaxCategory.BOOKS.getName(), taxYear);
        long eduUsed = (eduUsedRaw != null ? eduUsedRaw : 0L) + (booksUsedRaw != null ? booksUsedRaw : 0L);
        long eduRecommendedLimit = taxParameterService.getEducationLimit(taxYear);
        if (eduUsed < eduRecommendedLimit) {
            long eduRemaining = eduRecommendedLimit - eduUsed;
            long eduSaving = (long) Math.floor(eduRemaining * currentTax.taxRate());
            eduSaving += (long) (eduSaving * localTaxRate); // 지방세 포함 (지방세법 §92)
            recommendations.add(TaxSavingRecommendation.withUsage(
                    "EXPENSE",
                    "교육훈련비 경비 활용",
                    "인프런, Udemy, 기술서적, 컨퍼런스 경비 처리 가능 (소득세법 §19)",
                    eduRecommendedLimit, eduSaving, false, eduUsed));
        }

        long potentialTotal = recommendations.stream()
                .mapToLong(TaxSavingRecommendation::estimatedSaving).sum();

        // totalSummary (#27)
        long totalMax = recommendations.stream()
                .filter(r -> r.maxAmount() > 0)
                .mapToLong(TaxSavingRecommendation::maxAmount).sum();
        long totalUsed = recommendations.stream()
                .mapToLong(TaxSavingRecommendation::usedAmount).sum();
        long totalRemaining = Math.max(0, totalMax - totalUsed);
        double overallRate = totalMax == 0 ? 0.0
                : Math.round(totalUsed * 1000.0 / totalMax) / 10.0;
        var totalSummary = new TaxSavingResponse.TotalSavingSummary(
                totalMax, totalUsed, totalRemaining, overallRate);

        return new TaxSavingResponse(currentTax.finalTax(), recommendations, potentialTotal, totalSummary);
    }

    /**
     * 절세포인트 탭 전용 요약 API
     * — 한도 트래킹 (접대비, 차량유지비, 노란우산공제)
     * — 절세 가이드 (중소기업 특별세액감면, 기장세액공제)
     */
    public TaxSavingSummaryResponse getSummary(Long userId, int taxYear) {
        LocalDate start = LocalDate.of(taxYear, 1, 1);
        LocalDate end = LocalDate.of(taxYear, 12, 31);
        AggregateResult agg = bookEntryRepository.safeAggregate(userId, start, end);
        long totalRevenue = agg.totalIncome();
        long totalExpense = agg.businessExpense();
        long businessIncome = totalRevenue - totalExpense;

        // ── 한도 트래킹 ──

        // 1. 접대비 (소득세법 §35, 시행령 §78)
        Long entertainmentUsedRaw = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                userId, TaxCategory.ENTERTAINMENT.getName(), taxYear);
        long entertainmentUsed = entertainmentUsedRaw != null ? entertainmentUsedRaw : 0L;
        long entertainmentLimit = taxParameterService.getEntertainmentLimit(taxYear, totalRevenue);
        double entertainmentRatio = entertainmentLimit > 0
                ? Math.round(entertainmentUsed * 1000.0 / entertainmentLimit) / 1000.0 : 0.0;

        // 2. 차량유지비 (소득세법 §33의2, 시행령 §78의3)
        Long vehicleUsedRaw = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                userId, TaxCategory.VEHICLE.getName(), taxYear);
        long vehicleUsed = vehicleUsedRaw != null ? vehicleUsedRaw : 0L;
        long vehicleLimit = taxParameterService.getVehicleExpenseLimit(taxYear);
        double vehicleRatio = vehicleLimit > 0
                ? Math.round(vehicleUsed * 1000.0 / vehicleLimit) / 1000.0 : 0.0;

        // 3. 노란우산공제 (조특법 §86의3) — 소득공제 항목이므로 TaxReturn 공제내역에서 조회
        long noranPaid = getNoranPaidFromTaxReturn(userId, taxYear);
        long noranDeductionLimit = taxParameterService.getNoranLimit(taxYear, businessIncome);

        // 예상 절세액 = 납입액 × 적용세율 × (1 + 지방세율)
        Map<String, Long> baseDeductions = new HashMap<>();
        baseDeductions.put("기본공제_본인", taxParameterService.getBasicDeduction(taxYear));
        TaxCalculationResult currentTax = taxCalculationEngine.calculate(
                taxYear, totalRevenue, totalExpense, 0, baseDeductions);
        double localTaxRate = taxParameterService.getLocalTaxRate(taxYear);
        long effectivePaid = Math.min(noranPaid, noranDeductionLimit);
        long noranSaving = (long) Math.floor(effectivePaid * currentTax.taxRate());
        noranSaving += (long) (noranSaving * localTaxRate);

        var limitTracking = new LimitTracking(
                new TaxSavingSummaryResponse.LimitItem(entertainmentUsed, entertainmentLimit, entertainmentRatio),
                new TaxSavingSummaryResponse.LimitItem(vehicleUsed, vehicleLimit, vehicleRatio),
                new TaxSavingSummaryResponse.YellowUmbrellaItem(noranPaid, noranDeductionLimit, noranSaving));

        // ── 절세 가이드 ──

        long calculatedTax = currentTax.calculatedTax();

        // 4. 중소기업 특별세액감면 (조특법 §7)
        boolean smeEligible = false;
        double smeRate = 0.0;
        long smeAmount = 0;
        var profileOpt = businessProfileRepository.findByUserId(userId);
        if (profileOpt.isPresent()) {
            smeRate = taxParameterService.getSmeReductionRate(taxYear, profileOpt.get().getIndustryCode());
            smeEligible = smeRate > 0;
            if (smeEligible && calculatedTax > 0) {
                smeAmount = (long) Math.floor(calculatedTax * smeRate);
            }
        }

        // 5. 기장세액공제 (소득세법 §56조의2)
        double bookkeepingRate = taxParameterService.getBookkeepingCreditRate(taxYear);
        long bookkeepingLimit = taxParameterService.getBookkeepingCreditLimit(taxYear);
        long bookkeepingAmount = calculatedTax > 0
                ? Math.min((long) Math.floor(calculatedTax * bookkeepingRate), bookkeepingLimit) : 0;

        var taxReductions = new TaxReductions(
                new TaxSavingSummaryResponse.SmeReductionItem(smeEligible, smeRate, smeAmount),
                new TaxSavingSummaryResponse.BookkeepingCreditItem(true, bookkeepingRate, bookkeepingAmount));

        return new TaxSavingSummaryResponse(limitTracking, taxReductions);
    }

    /** TaxReturn의 deductionsJson에서 노란우산공제 납입액 조회 (소득공제 항목이므로 BookEntry에 없음) */
    private long getNoranPaidFromTaxReturn(Long userId, int taxYear) {
        return taxReturnRepository.findByUserIdAndTaxYear(userId, taxYear)
                .map(tr -> parseDeductionAmount(tr.getDeductionsJson(), NORAN_CATEGORY))
                .orElse(0L);
    }

    private long parseDeductionAmount(String deductionsJson, String key) {
        if (deductionsJson == null || deductionsJson.isBlank()) return 0L;
        try {
            Map<String, Long> map = objectMapper.readValue(
                    deductionsJson, new TypeReference<Map<String, Long>>() {});
            return map.getOrDefault(key, 0L);
        } catch (Exception e) {
            log.warn("deductionsJson 파싱 실패: {}", e.getMessage());
            return 0L;
        }
    }
}
