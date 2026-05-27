package com.ssafy.tax7i.ai.mapper;

import com.ssafy.tax7i.classification.entity.TaxCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI 세목 분류 결과(20개 한글 카테고리)를 BE TaxCategory enum으로 매핑
 */
@Slf4j
@Component
public class AiCategoryMapper {

    private static final Map<String, TaxCategory> MAPPING = Map.ofEntries(
            Map.entry("매출", TaxCategory.SALES),
            Map.entry("상품·원재료 매입", TaxCategory.PURCHASE),
            Map.entry("기초/기말 재고", TaxCategory.INVENTORY),
            Map.entry("급료", TaxCategory.SALARY),
            Map.entry("제세공과금", TaxCategory.TAX_DUES),
            Map.entry("임차료", TaxCategory.RENT),
            Map.entry("지급이자", TaxCategory.INTEREST),
            Map.entry("접대비", TaxCategory.ENTERTAINMENT),
            Map.entry("기부금", TaxCategory.DONATION),
            Map.entry("감가상각비", TaxCategory.DEPRECIATION),
            Map.entry("차량유지비", TaxCategory.VEHICLE),
            Map.entry("지급수수료", TaxCategory.SERVICE_FEE),
            Map.entry("소모품비", TaxCategory.SUPPLIES),
            Map.entry("복리후생비", TaxCategory.WELFARE),
            Map.entry("운반비", TaxCategory.SHIPPING),
            Map.entry("광고선전비", TaxCategory.ADVERTISING),
            Map.entry("여비교통비", TaxCategory.TRAVEL),
            Map.entry("기타경비", TaxCategory.OTHER_EXPENSE),   // AI: 공백 없음
            Map.entry("기타 경비", TaxCategory.OTHER_EXPENSE),  // BE: 공백 있음
            Map.entry("고정자산 매입", TaxCategory.ASSET_PURCHASE),
            Map.entry("고정자산 매도", TaxCategory.ASSET_SALE)
    );

    /**
     * AI 카테고리명 → TaxCategory enum 매핑.
     * 매핑 불가 시 OTHER_EXPENSE 반환.
     */
    public TaxCategory map(String aiCategoryName) {
        if (aiCategoryName == null || aiCategoryName.isBlank()) {
            return TaxCategory.OTHER_EXPENSE;
        }
        TaxCategory result = MAPPING.get(aiCategoryName.trim());
        if (result == null) {
            log.warn("AI 카테고리 매핑 실패, OTHER_EXPENSE 폴백: aiCategory={}", aiCategoryName);
            return TaxCategory.OTHER_EXPENSE;
        }
        return result;
    }

    public String mapToName(String aiCategoryName) {
        return map(aiCategoryName).getName();
    }

    public String mapToCode(String aiCategoryName) {
        return map(aiCategoryName).getCode();
    }
}
