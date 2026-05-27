package com.ssafy.tax7i.config;

import com.ssafy.tax7i.tax.entity.TaxBracket;
import com.ssafy.tax7i.tax.repository.TaxBracketRepository;
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
@Order(1)
public class TaxBracketDataInitializer implements CommandLineRunner {

    private final TaxBracketRepository taxBracketRepository;

    private static final int[] SEED_YEARS = {2025, 2026};

    @Override
    @Transactional
    public void run(String... args) {
        for (int year : SEED_YEARS) {
            initBrackets(year);
        }
    }

    private void initBrackets(int year) {
        if (!taxBracketRepository.findByYearOrderByBracketMinAsc(year).isEmpty()) {
            log.info("[TaxBracketDataInitializer] {}년 세율 구간 데이터가 이미 존재합니다. 건너뜁니다.", year);
            return;
        }

        log.info("[TaxBracketDataInitializer] {}년 종합소득세 세율 구간 데이터를 초기화합니다.", year);

        List<TaxBracket> brackets = List.of(
                TaxBracket.builder()
                        .year(year).bracketMin(0).bracketMax(14_000_000L)
                        .rate(0.06).progressiveDeduction(0L).build(),
                TaxBracket.builder()
                        .year(year).bracketMin(14_000_001L).bracketMax(50_000_000L)
                        .rate(0.15).progressiveDeduction(1_260_000L).build(),
                TaxBracket.builder()
                        .year(year).bracketMin(50_000_001L).bracketMax(88_000_000L)
                        .rate(0.24).progressiveDeduction(5_760_000L).build(),
                TaxBracket.builder()
                        .year(year).bracketMin(88_000_001L).bracketMax(150_000_000L)
                        .rate(0.35).progressiveDeduction(15_440_000L).build(),
                TaxBracket.builder()
                        .year(year).bracketMin(150_000_001L).bracketMax(300_000_000L)
                        .rate(0.38).progressiveDeduction(19_940_000L).build(),
                TaxBracket.builder()
                        .year(year).bracketMin(300_000_001L).bracketMax(500_000_000L)
                        .rate(0.40).progressiveDeduction(25_940_000L).build(),
                TaxBracket.builder()
                        .year(year).bracketMin(500_000_001L).bracketMax(1_000_000_000L)
                        .rate(0.42).progressiveDeduction(35_940_000L).build(),
                TaxBracket.builder()
                        .year(year).bracketMin(1_000_000_001L).bracketMax(9_999_999_999L)
                        .rate(0.45).progressiveDeduction(65_940_000L).build()
        );

        taxBracketRepository.saveAll(brackets);
        log.info("[TaxBracketDataInitializer] {}년 세율 구간 {}개 저장 완료.", year, brackets.size());
    }
}
