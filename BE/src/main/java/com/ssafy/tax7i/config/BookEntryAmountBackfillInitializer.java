package com.ssafy.tax7i.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code db/migration/V3__fix_book_entry_amounts.sql} 상당의 런타임 백필.
 *
 * <p>이 프로젝트는 Flyway/Liquibase가 없어 {@code db/migration/*.sql}이 자동 적용되지 않는다.
 * 간편장부 금액(income/expense/fixedAsset)을 원 결제금액({@code supply_price + vat_amount})으로
 * 정규화하는 V3 보정을 앱 기동 시 멱등하게 수행한다.
 *
 * <p>{@code WHERE ... <> supply_price + vat_amount} 가드로 이미 정규화된 행은 건드리지 않는다.
 * 따라서 신규/보정 완료 DB에서는 no-op이고, 레거시(부가세 제외 공급가액이 저장된) 행만 1회 보정된다.
 * 볼륨이 이미 초기화되어 initdb가 재실행되지 않는 경우에도 이 러너가 보정을 보장한다.
 *
 * <p>V2(2025년 세금 기준 데이터)는 {@code 02_seed_data.sql} + {@link TaxBracketDataInitializer}·
 * {@link TaxParameterDataInitializer}·{@link TaxDeadlineDataInitializer}가 이미 멱등 적용하므로 별도 불필요.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(10)
public class BookEntryAmountBackfillInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        int income = jdbcTemplate.update(
                "UPDATE book_entries SET income_amount = supply_price + vat_amount "
                        + "WHERE entry_type = 'INCOME' AND income_amount <> supply_price + vat_amount");
        int expense = jdbcTemplate.update(
                "UPDATE book_entries SET expense_amount = supply_price + vat_amount "
                        + "WHERE entry_type = 'EXPENSE' AND expense_amount <> supply_price + vat_amount");
        int asset = jdbcTemplate.update(
                "UPDATE book_entries SET fixed_asset_amount = supply_price + vat_amount "
                        + "WHERE entry_type = 'ASSET' AND fixed_asset_amount <> supply_price + vat_amount");

        int total = income + expense + asset;
        if (total > 0) {
            log.info("[BookEntryAmountBackfill] 간편장부 금액 백필 완료: 총 {}건 (income={}, expense={}, asset={})",
                    total, income, expense, asset);
        } else {
            log.info("[BookEntryAmountBackfill] 보정 대상 없음 (이미 정규화됨).");
        }
    }
}
