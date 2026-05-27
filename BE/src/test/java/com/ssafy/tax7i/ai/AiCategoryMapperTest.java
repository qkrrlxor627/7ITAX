package com.ssafy.tax7i.ai;

import com.ssafy.tax7i.ai.mapper.AiCategoryMapper;
import com.ssafy.tax7i.classification.entity.TaxCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AiCategoryMapperTest {

    private final AiCategoryMapper mapper = new AiCategoryMapper();

    @ParameterizedTest
    @DisplayName("AI 카테고리 20개 → BE TaxCategory 매핑")
    @CsvSource({
            "매출, SALES",
            "상품·원재료 매입, PURCHASE",
            "기초/기말 재고, INVENTORY",
            "급료, SALARY",
            "제세공과금, TAX_DUES",
            "임차료, RENT",
            "지급이자, INTEREST",
            "접대비, ENTERTAINMENT",
            "기부금, DONATION",
            "감가상각비, DEPRECIATION",
            "차량유지비, VEHICLE",
            "지급수수료, SERVICE_FEE",
            "소모품비, SUPPLIES",
            "복리후생비, WELFARE",
            "운반비, SHIPPING",
            "광고선전비, ADVERTISING",
            "여비교통비, TRAVEL",
            "고정자산 매입, ASSET_PURCHASE",
            "고정자산 매도, ASSET_SALE",
    })
    void AI_카테고리_매핑(String aiCategory, String expectedEnum) {
        TaxCategory result = mapper.map(aiCategory);
        assertThat(result).isEqualTo(TaxCategory.valueOf(expectedEnum));
    }

    @Test
    @DisplayName("AI '기타경비'(공백없음) → BE '기타 경비'(공백있음) 매핑")
    void 기타경비_공백차이_매핑() {
        assertThat(mapper.map("기타경비")).isEqualTo(TaxCategory.OTHER_EXPENSE);
        assertThat(mapper.map("기타 경비")).isEqualTo(TaxCategory.OTHER_EXPENSE);
        assertThat(mapper.mapToName("기타경비")).isEqualTo("기타 경비");
    }

    @Test
    @DisplayName("미매핑 카테고리 → OTHER_EXPENSE 기본값")
    void 미매핑_기본값() {
        assertThat(mapper.map("알수없는카테고리")).isEqualTo(TaxCategory.OTHER_EXPENSE);
        assertThat(mapper.map(null)).isEqualTo(TaxCategory.OTHER_EXPENSE);
        assertThat(mapper.map("")).isEqualTo(TaxCategory.OTHER_EXPENSE);
        assertThat(mapper.map("  ")).isEqualTo(TaxCategory.OTHER_EXPENSE);
    }

    @Test
    @DisplayName("mapToCode 정상 동작")
    void 코드매핑() {
        assertThat(mapper.mapToCode("접대비")).isEqualTo("08");
        assertThat(mapper.mapToCode("매출")).isEqualTo("01");
    }
}
