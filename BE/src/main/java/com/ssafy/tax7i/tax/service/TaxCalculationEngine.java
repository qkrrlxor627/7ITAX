package com.ssafy.tax7i.tax.service;

import com.ssafy.tax7i.auth.repository.BusinessProfileRepository;
import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.classification.entity.TaxCategory;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.dto.TaxCalculationResult;
import com.ssafy.tax7i.tax.entity.TaxBracket;
import com.ssafy.tax7i.tax.repository.TaxBracketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaxCalculationEngine {

    private final TaxBracketRepository taxBracketRepository;
    private final BookEntryRepository bookEntryRepository;
    private final TaxParameterService taxParameterService;
    private final BusinessProfileRepository businessProfileRepository;

    @Cacheable(value = "taxBrackets", key = "#year")
    public List<TaxBracket> loadBrackets(int year) {
        return taxBracketRepository.findByYearOrderByBracketMinAsc(year);
    }

    /** 세율 구간 리스트로부터 산출세액 계산 (TaxEstimationService 등 재활용) */
    public long calculateIncomeTaxFromBrackets(long taxableIncome, List<TaxBracket> brackets) {
        if (taxableIncome <= 0) return 0;
        for (TaxBracket bracket : brackets) {
            if (taxableIncome >= bracket.getBracketMin() && taxableIncome <= bracket.getBracketMax()) {
                return (long) Math.floor(taxableIncome * bracket.getRate() - bracket.getProgressiveDeduction());
            }
        }
        return 0;
    }

    /** 과세표준에 해당하는 세율 구간 문자열 반환 */
    public String findBracketLabel(long taxableIncome, List<TaxBracket> brackets) {
        for (TaxBracket bracket : brackets) {
            if (taxableIncome >= bracket.getBracketMin() && taxableIncome <= bracket.getBracketMax()) {
                long ratePercent = Math.round(bracket.getRate() * 100);
                return ratePercent + "%";
            }
        }
        if (!brackets.isEmpty()) {
            long lastRate = Math.round(brackets.get(brackets.size() - 1).getRate() * 100);
            return lastRate + "%";
        }
        return "0%";
    }

    /**
     * 세액공제/감면 자동 산출 (조특법 §7, 소득세법 §56조의2)
     *
     * 국세청 적용 순서:
     *   산출세액 → (-) 중소기업특별세액감면 → (-) 기장세액공제 → 결정세액
     * 두 공제 모두 산출세액 기준으로 계산하므로 합산하여 taxCredits에 전달.
     */
    public Map<String, Long> computeTaxCredits(Long userId, int taxYear, long calculatedTax) {
        if (calculatedTax <= 0) return Collections.emptyMap();

        Map<String, Long> credits = new HashMap<>();

        // 1. 중소기업 특별세액감면 (조특법 §7)
        businessProfileRepository.findByUserId(userId).ifPresent(profile -> {
            double smeRate = taxParameterService.getSmeReductionRate(taxYear, profile.getIndustryCode());
            if (smeRate > 0) {
                long smeReduction = (long) Math.floor(calculatedTax * smeRate);
                credits.put("중소기업특별세액감면", smeReduction);
            }
        });

        // 2. 기장세액공제 (소득세법 §56조의2) — 간편장부 기장 시 산출세액 × 20%, 한도 100만원
        double bkRate = taxParameterService.getBookkeepingCreditRate(taxYear);
        long bkLimit = taxParameterService.getBookkeepingCreditLimit(taxYear);
        long bookkeepingCredit = Math.min((long) Math.floor(calculatedTax * bkRate), bkLimit);
        credits.put("기장세액공제", bookkeepingCredit);

        return credits;
    }

    /** 장부 집계 + 세금 계산을 한 번에 수행 (Controller → Service 레이어 규칙 준수) */
    public TaxCalculationResult calculateForUser(Long userId, int taxYear,
                                                  Long prepaidTaxInput, Map<String, Long> deductionsInput) {
        LocalDate start = LocalDate.of(taxYear, 1, 1);
        LocalDate end = LocalDate.of(taxYear, 12, 31);
        AggregateResult agg = bookEntryRepository.safeAggregate(userId, start, end);
        long totalRevenue = agg.totalIncome();
        long totalExpense = agg.businessExpense();

        // 소득세법 §33·§35·§33의2: 세목별 한도 초과 경비 조정
        long adjustment = computeExpenseAdjustment(userId, taxYear, totalRevenue);
        long adjustedExpense = Math.max(0, totalExpense - adjustment);

        long prepaidTax = prepaidTaxInput != null ? prepaidTaxInput : 0L;
        Map<String, Long> deductions = deductionsInput != null ? new HashMap<>(deductionsInput) : new HashMap<>();
        // 기본공제 본인 (소득세법 §50) — 모든 거주자에게 무조건 적용
        deductions.putIfAbsent("기본공제_본인", taxParameterService.getBasicDeduction(taxYear));

        // 세액공제/감면 자동 산출 (조특법 §7, 소득세법 §56조의2)
        TaxCalculationResult base = calculate(taxYear, totalRevenue, adjustedExpense, 0, deductions);
        Map<String, Long> taxCredits = computeTaxCredits(userId, taxYear, base.calculatedTax());

        return calculate(taxYear, totalRevenue, adjustedExpense, prepaidTax, deductions, taxCredits);
    }

    /**
     * 세목별 한도 초과 경비 조정액 산출 (소득세 신고 2단계)
     *
     * 소득세법 §35 접대비: min(실지출액, 기본한도 + 총수입×0.2%) → 초과분 불산입
     * 소득세법 §33의2 차량유지비: min(실지출액, 연 1,500만원) → 초과분 불산입
     *
     * @return totalExpense에서 차감해야 할 초과 경비 합계
     */
    public long computeExpenseAdjustment(Long userId, int taxYear, long totalRevenue) {
        long adjustment = 0;

        // 1. 접대비 한도 초과분 (소득세법 §35, 시행령 §79)
        Long entertainmentRaw = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                userId, TaxCategory.ENTERTAINMENT.getName(), taxYear);
        long entertainmentUsed = entertainmentRaw != null ? entertainmentRaw : 0L;
        if (entertainmentUsed > 0) {
            long entertainmentLimit = taxParameterService.getEntertainmentLimit(taxYear, totalRevenue);
            if (entertainmentUsed > entertainmentLimit) {
                adjustment += (entertainmentUsed - entertainmentLimit);
            }
        }

        // 2. 차량유지비 한도 초과분 (소득세법 §33의2, 시행령 §78의3)
        Long vehicleRaw = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                userId, TaxCategory.VEHICLE.getName(), taxYear);
        long vehicleUsed = vehicleRaw != null ? vehicleRaw : 0L;
        if (vehicleUsed > 0) {
            long vehicleLimit = taxParameterService.getVehicleExpenseLimit(taxYear);
            if (vehicleUsed > vehicleLimit) {
                adjustment += (vehicleUsed - vehicleLimit);
            }
        }

        return adjustment;
    }

    /** 하위호환 오버로드 — 세액공제 없이 호출 */
    public TaxCalculationResult calculate(int taxYear, long totalRevenue, long totalExpense,
                                          long prepaidTax, Map<String, Long> deductions) {
        return calculate(taxYear, totalRevenue, totalExpense, prepaidTax, deductions, null);
    }

    /**
     * 종합소득세 계산 (소득공제 + 세액공제 분리)
     * @param deductions  소득공제 항목 (과세표준에서 차감: 기본공제, 노란우산공제 등)
     * @param taxCredits  세액공제 항목 (산출세액에서 차감: 연금저축 세액공제 등)
     */
    public TaxCalculationResult calculate(int taxYear, long totalRevenue, long totalExpense,
                                          long prepaidTax, Map<String, Long> deductions,
                                          Map<String, Long> taxCredits) {

        // 1. 소득금액 = 총수입 - 총경비
        long incomeAmount = totalRevenue - totalExpense;

        // 2. 총 소득공제액 합산
        long totalDeductions = 0L;
        if (deductions != null) {
            for (Long value : deductions.values()) {
                if (value != null) {
                    totalDeductions += value;
                }
            }
        }

        // 3. 과세표준 = max(0, 소득금액 - 총소득공제액)
        long taxableIncome = Math.max(0, incomeAmount - totalDeductions);

        // 4. 세율 구간 로드
        List<TaxBracket> brackets = loadBrackets(taxYear);
        if (brackets.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    taxYear + "년도 세율 구간 데이터가 없습니다.");
        }

        // 5. 적용 세율 및 산출세액 계산
        double taxRate = 0.0;
        long calculatedTax = 0L;
        boolean matched = false;

        for (TaxBracket bracket : brackets) {
            if (taxableIncome >= bracket.getBracketMin() && taxableIncome <= bracket.getBracketMax()) {
                taxRate = bracket.getRate();
                calculatedTax = (long) Math.floor(taxableIncome * bracket.getRate() - bracket.getProgressiveDeduction());
                matched = true;
                break;
            }
        }

        if (!matched && taxableIncome > 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "과세표준에 해당하는 세율 구간이 없습니다.");
        }

        // 6. 총 세액공제액 합산
        long totalTaxCredits = 0L;
        if (taxCredits != null) {
            for (Long value : taxCredits.values()) {
                if (value != null) {
                    totalTaxCredits += value;
                }
            }
        }

        // 7. 결정세액 = max(0, 산출세액 - 세액공제)
        long determinedTax = Math.max(0, calculatedTax - totalTaxCredits);

        // 8. 최종 납부/환급 세액 = 결정세액 - 기납부세액
        long finalTax = determinedTax - prepaidTax;

        // 9. 지방소득세 = floor(결정세액 × 지방세율) (지방세법 §92)
        double localTaxRate = taxParameterService.getLocalTaxRate(taxYear);
        long localTax = (long) Math.floor(determinedTax * localTaxRate);

        // 10. 환급 여부
        boolean isRefund = finalTax < 0;

        return new TaxCalculationResult(
                totalRevenue,
                totalExpense,
                incomeAmount,
                totalDeductions,
                taxableIncome,
                taxRate,
                calculatedTax,
                totalTaxCredits,
                determinedTax,
                prepaidTax,
                finalTax,
                localTax,
                isRefund
        );
    }
}
