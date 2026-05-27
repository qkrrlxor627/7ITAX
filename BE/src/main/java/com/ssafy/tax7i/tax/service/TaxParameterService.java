package com.ssafy.tax7i.tax.service;

import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.entity.TaxParameter;
import com.ssafy.tax7i.tax.repository.TaxParameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxParameterService {

    private final TaxParameterRepository taxParameterRepository;

    @Cacheable(value = "taxParameters", key = "#year + ':' + #category")
    public Map<String, String> getParams(int year, String category) {
        List<TaxParameter> params = taxParameterRepository.findByYearAndCategory(year, category);
        if (params.isEmpty()) {
            log.info("{}년 {} 파라미터 없음, {}년 폴백 시도", year, category, year - 1);
            params = taxParameterRepository.findByYearAndCategory(year - 1, category);
        }
        if (params.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    year + "년 " + category + " 세금 파라미터가 없습니다.");
        }
        return params.stream()
                .collect(Collectors.toMap(TaxParameter::getParamKey, TaxParameter::getParamValue));
    }

    private String requireParam(Map<String, String> params, String category, String key) {
        String value = params.get(key);
        if (value == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    category + " 파라미터에 " + key + " 값이 없습니다.");
        }
        return value;
    }

    private long parseLong(String category, String key, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    category + "." + key + " 값이 올바른 숫자가 아닙니다: " + value);
        }
    }

    private double parseDouble(String category, String key, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    category + "." + key + " 값이 올바른 숫자가 아닙니다: " + value);
        }
    }

    /** 지방소득세율 (지방세법 §92) */
    public double getLocalTaxRate(int year) {
        Map<String, String> params = getParams(year, "LOCAL_TAX");
        return parseDouble("LOCAL_TAX", "rate", requireParam(params, "LOCAL_TAX", "rate"));
    }

    /** 부가가치세율 (부가가치세법 §30) */
    public double getVatRate(int year) {
        Map<String, String> params = getParams(year, "VAT");
        return parseDouble("VAT", "rate", requireParam(params, "VAT", "rate"));
    }

    /** 기본공제 본인 (소득세법 §50) */
    public long getBasicDeduction(int year) {
        Map<String, String> params = getParams(year, "BASIC_DEDUCTION");
        return parseLong("BASIC_DEDUCTION", "personal", requireParam(params, "BASIC_DEDUCTION", "personal"));
    }

    /** 노란우산공제 한도 (소기업소상공인공제부금법) — 사업소득금액 기준 3단계 */
    public long getNoranLimit(int year, long businessIncome) {
        Map<String, String> params = getParams(year, "NORAN_DEDUCTION");
        long threshold = parseLong("NORAN_DEDUCTION", "threshold", requireParam(params, "NORAN_DEDUCTION", "threshold"));
        long thresholdHigh = parseLong("NORAN_DEDUCTION", "threshold_high", requireParam(params, "NORAN_DEDUCTION", "threshold_high"));
        if (businessIncome <= threshold) {
            return parseLong("NORAN_DEDUCTION", "limit_low", requireParam(params, "NORAN_DEDUCTION", "limit_low"));
        } else if (businessIncome <= thresholdHigh) {
            return parseLong("NORAN_DEDUCTION", "limit_high", requireParam(params, "NORAN_DEDUCTION", "limit_high"));
        } else {
            return parseLong("NORAN_DEDUCTION", "limit_top", requireParam(params, "NORAN_DEDUCTION", "limit_top"));
        }
    }

    /** 연금저축 세액공제율 (소득세법 §59조의3) — 종합소득금액 기준 */
    public double getPensionCreditRate(int year, long comprehensiveIncome) {
        Map<String, String> params = getParams(year, "PENSION_CREDIT");
        long threshold = parseLong("PENSION_CREDIT", "threshold", requireParam(params, "PENSION_CREDIT", "threshold"));
        return comprehensiveIncome <= threshold
                ? parseDouble("PENSION_CREDIT", "rate_low", requireParam(params, "PENSION_CREDIT", "rate_low"))
                : parseDouble("PENSION_CREDIT", "rate_high", requireParam(params, "PENSION_CREDIT", "rate_high"));
    }

    /** 연금저축 연간 한도 */
    public long getPensionLimit(int year) {
        Map<String, String> params = getParams(year, "PENSION_CREDIT");
        return parseLong("PENSION_CREDIT", "limit", requireParam(params, "PENSION_CREDIT", "limit"));
    }

    /**
     * 중소기업 특별세액감면율 (조특법 §7②)
     * @param industryCode 업종코드 (예: "722000")
     * @return 감면율 (해당 업종이면 0.30, 비해당이면 0.0)
     */
    public double getSmeReductionRate(int year, String industryCode) {
        if (industryCode == null || industryCode.length() < 2) return 0.0;
        Map<String, String> params = getParams(year, "SME_REDUCTION");
        String eligibleCodes = requireParam(params, "SME_REDUCTION", "eligible_codes");
        String prefix = industryCode.substring(0, 2);
        for (String code : eligibleCodes.split(",")) {
            if (code.trim().equals(prefix)) {
                return parseDouble("SME_REDUCTION", "rate", requireParam(params, "SME_REDUCTION", "rate"));
            }
        }
        return 0.0;
    }

    /** 기장세액공제율 (소득세법 §56조의2) — 간편장부 기장 시 산출세액의 20% */
    public double getBookkeepingCreditRate(int year) {
        Map<String, String> params = getParams(year, "BOOKKEEPING_CREDIT");
        return parseDouble("BOOKKEEPING_CREDIT", "rate", requireParam(params, "BOOKKEEPING_CREDIT", "rate"));
    }

    /** 기장세액공제 한도 (소득세법 §56조의2) — 연 100만원 */
    public long getBookkeepingCreditLimit(int year) {
        Map<String, String> params = getParams(year, "BOOKKEEPING_CREDIT");
        return parseLong("BOOKKEEPING_CREDIT", "limit", requireParam(params, "BOOKKEEPING_CREDIT", "limit"));
    }

    /** 접대비 연간 기본한도 (소득세법 §35) — 기본한도만 반환 (하위호환) */
    public long getEntertainmentLimit(int year) {
        Map<String, String> params = getParams(year, "ENTERTAINMENT");
        return parseLong("ENTERTAINMENT", "annual_limit", requireParam(params, "ENTERTAINMENT", "annual_limit"));
    }

    /**
     * 접대비 연간 한도 (소득세법 §35, 시행령 §79)
     * = 기본한도(2,400만원) + 총수입금액 × 수입비례율(0.2%)
     */
    public long getEntertainmentLimit(int year, long totalRevenue) {
        Map<String, String> params = getParams(year, "ENTERTAINMENT");
        long baseLimit = parseLong("ENTERTAINMENT", "annual_limit", requireParam(params, "ENTERTAINMENT", "annual_limit"));
        double revenueRate = parseDouble("ENTERTAINMENT", "revenue_rate",
                requireParam(params, "ENTERTAINMENT", "revenue_rate"));
        return baseLimit + (long) Math.floor(totalRevenue * revenueRate);
    }

    /** 차량유지비 연간 한도 (소득세법 §33의2) */
    public long getVehicleExpenseLimit(int year) {
        Map<String, String> params = getParams(year, "VEHICLE");
        return parseLong("VEHICLE", "annual_limit", requireParam(params, "VEHICLE", "annual_limit"));
    }

    /** 업무전용보험 미가입 시 강제 업무사용비율 (소득세법 시행령 §78의3) */
    public double getVehicleNoInsuranceRatio(int year) {
        Map<String, String> params = getParams(year, "VEHICLE");
        return parseDouble("VEHICLE", "no_insurance_ratio", requireParam(params, "VEHICLE", "no_insurance_ratio"));
    }

    /** 사업소득 원천징수율 (소득세법 §129①5) — DB 미설정 시 기본값 3% */
    public double getWithholdingRate(int year) {
        try {
            Map<String, String> params = getParams(year, "WITHHOLDING");
            return parseDouble("WITHHOLDING", "business_income_rate",
                    requireParam(params, "WITHHOLDING", "business_income_rate"));
        } catch (BusinessException e) {
            log.info("WITHHOLDING 파라미터 미설정, 기본값 0.03 사용 ({}년)", year);
            return 0.03;
        }
    }

    /** 교육훈련비 추천한도 (소득세법 §19) */
    public long getEducationLimit(int year) {
        Map<String, String> params = getParams(year, "EDUCATION");
        return parseLong("EDUCATION", "recommended_limit", requireParam(params, "EDUCATION", "recommended_limit"));
    }
}
