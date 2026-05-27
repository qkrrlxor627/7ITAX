package com.ssafy.tax7i.config;

import com.ssafy.tax7i.tax.entity.TaxParameter;
import com.ssafy.tax7i.tax.repository.TaxParameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class TaxParameterDataInitializer implements CommandLineRunner {

    private final TaxParameterRepository taxParameterRepository;

    private static final int[] SEED_YEARS = {2025, 2026};

    @Override
    @Transactional
    public void run(String... args) {
        for (int year : SEED_YEARS) {
            initParams(year);
        }
    }

    private void initParams(int year) {
        if (taxParameterRepository.countByYear(year) > 0) {
            log.info("[TaxParameterDataInitializer] {}년 세금 파라미터가 이미 존재합니다. 건너뜁니다.", year);
            return;
        }

        log.info("[TaxParameterDataInitializer] {}년 세금 파라미터를 초기화합니다.", year);

        List<TaxParameter> params = List.of(
                // 지방소득세율 (지방세법 §92)
                TaxParameter.builder()
                        .year(year).category("LOCAL_TAX").paramKey("rate").paramValue("0.10")
                        .description("지방소득세율").legalBasis("지방세법 §92").build(),

                // 부가가치세율 (부가가치세법 §30)
                TaxParameter.builder()
                        .year(year).category("VAT").paramKey("rate").paramValue("0.10")
                        .description("부가가치세율").legalBasis("부가가치세법 §30").build(),

                // 기본공제 본인 (소득세법 §50)
                TaxParameter.builder()
                        .year(year).category("BASIC_DEDUCTION").paramKey("personal").paramValue("1500000")
                        .description("기본공제 본인").legalBasis("소득세법 §50").build(),

                // 노란우산공제 (소기업소상공인공제부금법) — 사업소득금액 기준 3단계
                TaxParameter.builder()
                        .year(year).category("NORAN_DEDUCTION").paramKey("threshold").paramValue("40000000")
                        .description("노란우산공제 소득분기점 (4천만원)").legalBasis("소기업소상공인공제부금법").build(),
                TaxParameter.builder()
                        .year(year).category("NORAN_DEDUCTION").paramKey("threshold_high").paramValue("100000000")
                        .description("노란우산공제 소득분기점 (1억원)").legalBasis("소기업소상공인공제부금법").build(),
                TaxParameter.builder()
                        .year(year).category("NORAN_DEDUCTION").paramKey("limit_low").paramValue("5000000")
                        .description("사업소득 4천만 이하 공제한도").legalBasis("소기업소상공인공제부금법").build(),
                TaxParameter.builder()
                        .year(year).category("NORAN_DEDUCTION").paramKey("limit_high").paramValue("3000000")
                        .description("사업소득 4천만~1억 공제한도").legalBasis("소기업소상공인공제부금법").build(),
                TaxParameter.builder()
                        .year(year).category("NORAN_DEDUCTION").paramKey("limit_top").paramValue("2000000")
                        .description("사업소득 1억 초과 공제한도").legalBasis("소기업소상공인공제부금법").build(),

                // 연금저축 세액공제 (소득세법 §59조의3)
                TaxParameter.builder()
                        .year(year).category("PENSION_CREDIT").paramKey("threshold").paramValue("45000000")
                        .description("연금저축 소득분기점 (종합소득금액 기준, 사업소득자)").legalBasis("소득세법 §59조의3").build(),
                TaxParameter.builder()
                        .year(year).category("PENSION_CREDIT").paramKey("rate_low").paramValue("0.15")
                        .description("소득 이하 세액공제율").legalBasis("소득세법 §59조의3").build(),
                TaxParameter.builder()
                        .year(year).category("PENSION_CREDIT").paramKey("rate_high").paramValue("0.12")
                        .description("소득 초과 세액공제율").legalBasis("소득세법 §59조의3").build(),
                TaxParameter.builder()
                        .year(year).category("PENSION_CREDIT").paramKey("limit").paramValue("6000000")
                        .description("연금저축 연간 한도").legalBasis("소득세법 §59조의3").build(),

                // 중소기업 특별세액감면 (조특법 §7)
                TaxParameter.builder()
                        .year(year).category("SME_REDUCTION").paramKey("rate").paramValue("0.30")
                        .description("소기업 세액감면율 (지식기반산업)").legalBasis("조특법 §7②").build(),
                TaxParameter.builder()
                        .year(year).category("SME_REDUCTION").paramKey("eligible_codes").paramValue("72,62,63,58")
                        .description("감면대상 업종코드 앞2자리 (SW,IT,정보통신,출판)").legalBasis("조특법 §7①").build(),

                // 기장세액공제 (소득세법 §56조의2)
                TaxParameter.builder()
                        .year(year).category("BOOKKEEPING_CREDIT").paramKey("rate").paramValue("0.20")
                        .description("간편장부 기장 세액공제율 (산출세액의 20%)").legalBasis("소득세법 §56조의2").build(),
                TaxParameter.builder()
                        .year(year).category("BOOKKEEPING_CREDIT").paramKey("limit").paramValue("1000000")
                        .description("기장세액공제 한도 (연 100만원)").legalBasis("소득세법 §56조의2").build(),

                // 접대비 (소득세법 §35)
                TaxParameter.builder()
                        .year(year).category("ENTERTAINMENT").paramKey("annual_limit").paramValue("24000000")
                        .description("접대비 연간 기본한도").legalBasis("소득세법 §35, 시행령 §78").build(),
                TaxParameter.builder()
                        .year(year).category("ENTERTAINMENT").paramKey("revenue_rate").paramValue("0.002")
                        .description("접대비 수입금액 비례 추가한도율 (수입×0.2%)").legalBasis("소득세법 시행령 §79").build(),

                // 차량유지비 (소득세법 §33의2)
                TaxParameter.builder()
                        .year(year).category("VEHICLE").paramKey("annual_limit").paramValue("15000000")
                        .description("차량유지비 연간 한도").legalBasis("소득세법 §33의2").build(),
                TaxParameter.builder()
                        .year(year).category("VEHICLE").paramKey("no_insurance_ratio").paramValue("0.5")
                        .description("업무전용보험 미가입 시 업무사용비율 강제").legalBasis("소득세법 시행령 §78의3").build(),

                // 교육훈련비 (소득세법 §19)
                TaxParameter.builder()
                        .year(year).category("EDUCATION").paramKey("recommended_limit").paramValue("1500000")
                        .description("교육훈련비 추천한도").legalBasis("소득세법 §19").build()
        );

        taxParameterRepository.saveAll(params);
        log.info("[TaxParameterDataInitializer] {}년 세금 파라미터 {}개 저장 완료.", year, params.size());
    }
}
