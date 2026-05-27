package com.ssafy.tax7i.bookentry.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AggregateResultTest {

    @Test
    @DisplayName("null 입력 → 모든 필드 0")
    void from_nullInput_returnsZeros() {
        AggregateResult result = AggregateResult.from(null);

        assertThat(result.totalIncome()).isZero();
        assertThat(result.totalExpense()).isZero();
        assertThat(result.totalAsset()).isZero();
        assertThat(result.businessExpense()).isZero();
    }

    @Test
    @DisplayName("빈 배열 → 모든 필드 0")
    void from_emptyArray_returnsZeros() {
        AggregateResult result = AggregateResult.from(new Object[]{});

        assertThat(result.totalIncome()).isZero();
        assertThat(result.totalExpense()).isZero();
    }

    @Test
    @DisplayName("정상 Object[] → 올바르게 추출")
    void from_normalArray_extractsCorrectly() {
        Object[] agg = {50_000_000L, 20_000_000L, 3_000_000L, 15_000_000L};

        AggregateResult result = AggregateResult.from(agg);

        assertThat(result.totalIncome()).isEqualTo(50_000_000L);
        assertThat(result.totalExpense()).isEqualTo(20_000_000L);
        assertThat(result.totalAsset()).isEqualTo(3_000_000L);
        assertThat(result.businessExpense()).isEqualTo(15_000_000L);
    }

    @Test
    @DisplayName("중첩 Object[][] → unwrap 후 올바르게 추출")
    void from_nestedArray_unwrapsCorrectly() {
        Object[] inner = {100L, 200L, 300L, 400L};
        Object[] outer = {inner};

        AggregateResult result = AggregateResult.from(outer);

        assertThat(result.totalIncome()).isEqualTo(100L);
        assertThat(result.totalExpense()).isEqualTo(200L);
        assertThat(result.totalAsset()).isEqualTo(300L);
        assertThat(result.businessExpense()).isEqualTo(400L);
    }

    @Test
    @DisplayName("null 원소 포함 → 해당 필드만 0")
    void from_nullElements_treatedAsZero() {
        Object[] agg = {1000L, null, 3000L, null};

        AggregateResult result = AggregateResult.from(agg);

        assertThat(result.totalIncome()).isEqualTo(1000L);
        assertThat(result.totalExpense()).isZero();
        assertThat(result.totalAsset()).isEqualTo(3000L);
        assertThat(result.businessExpense()).isZero();
    }

    @Test
    @DisplayName("부분 배열 (4개 미만) → 부족한 인덱스는 0")
    void from_partialArray_returnsZeroForMissing() {
        Object[] agg = {5000L, 3000L};

        AggregateResult result = AggregateResult.from(agg);

        assertThat(result.totalIncome()).isEqualTo(5000L);
        assertThat(result.totalExpense()).isEqualTo(3000L);
        assertThat(result.totalAsset()).isZero();
        assertThat(result.businessExpense()).isZero();
    }

    @Test
    @DisplayName("다양한 Number 타입 (Integer, BigDecimal) → long 변환")
    void from_mixedNumericTypes_convertsCorrectly() {
        Object[] agg = {Integer.valueOf(1000), BigDecimal.valueOf(2000), 3000L, Double.valueOf(4000)};

        AggregateResult result = AggregateResult.from(agg);

        assertThat(result.totalIncome()).isEqualTo(1000L);
        assertThat(result.totalExpense()).isEqualTo(2000L);
        assertThat(result.totalAsset()).isEqualTo(3000L);
        assertThat(result.businessExpense()).isEqualTo(4000L);
    }
}
