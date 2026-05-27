package com.ssafy.tax7i.classification;

import com.ssafy.tax7i.classification.entity.TaxCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MccClassificationTest {

    @Test
    @DisplayName("TaxCategory: 세목 코드/이름 체계 검증")
    void taxCategoryCodeAndName() {
        assertThat(TaxCategory.SALES.getCode()).isEqualTo("01");
        assertThat(TaxCategory.SALES.getName()).isEqualTo("매출");
        assertThat(TaxCategory.SERVICE_FEE.getCode()).isEqualTo("12");
        assertThat(TaxCategory.SERVICE_FEE.getName()).isEqualTo("지급수수료");
        assertThat(TaxCategory.ENTERTAINMENT.getCode()).isEqualTo("08");
        assertThat(TaxCategory.ENTERTAINMENT.getName()).isEqualTo("접대비");
        assertThat(TaxCategory.DEPRECIATION.getCode()).isEqualTo("10");
        assertThat(TaxCategory.DEPRECIATION.getName()).isEqualTo("감가상각비");
        assertThat(TaxCategory.ASSET_SALE.getCode()).isEqualTo("20");
    }

    @Test
    @DisplayName("TaxCategory: 전체 세목 코드 유일성 검증")
    void taxCategoryCodesAreUnique() {
        long uniqueCodeCount = java.util.Arrays.stream(TaxCategory.values())
                .map(TaxCategory::getCode)
                .distinct()
                .count();
        assertThat(uniqueCodeCount).isEqualTo(TaxCategory.values().length);
    }

    @Test
    @DisplayName("TaxCategory: 주요 세목 enum 이름 매칭")
    void taxCategoryEnumNames() {
        assertThat(TaxCategory.valueOf("TRAVEL").getName()).isEqualTo("여비교통비");
        assertThat(TaxCategory.valueOf("VEHICLE").getName()).isEqualTo("차량유지비");
        assertThat(TaxCategory.valueOf("SUPPLIES").getName()).isEqualTo("소모품비");
        assertThat(TaxCategory.valueOf("ADVERTISING").getName()).isEqualTo("광고선전비");
        assertThat(TaxCategory.valueOf("SHIPPING").getName()).isEqualTo("운반비");
        assertThat(TaxCategory.valueOf("RENT").getName()).isEqualTo("임차료");
        assertThat(TaxCategory.valueOf("OTHER_EXPENSE").getName()).isEqualTo("기타 경비");
    }
}
