package com.ssafy.tax7i.bookentry.repository;

/**
 * findLearnedCategory 쿼리 결과를 타입 안전하게 매핑하는 인터페이스 프로젝션.
 * Object[] 캐스팅 시 NPE 방지 및 가독성 개선.
 */
public interface LearnedCategoryProjection {
    String getCategoryCode();
    String getCategoryName();
    Boolean getIsVatDeductible();
    Long getCount();
}
