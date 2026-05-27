package com.ssafy.tax7i.classification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 간편장부 세목 (1인 IT개발자 기준)
 */
@Getter
@RequiredArgsConstructor
public enum TaxCategory {

    // A. 수입
    SALES("01", "매출", "사업수입"),

    // B. 매출원가
    PURCHASE("02", "상품·원재료 매입", "매출원가 관련 매입"),
    INVENTORY("03", "기초/기말 재고", "재고자산 증감"),

    // C. 일반관리비·판매비
    SALARY("04", "급료", "직원 급여·임금"),
    TAX_DUES("05", "제세공과금", "사업 관련 세금·공과금"),
    RENT("06", "임차료", "사무실·장비 대여료"),
    INTEREST("07", "지급이자", "사업자금 차입 이자"),
    ENTERTAINMENT("08", "접대비", "거래처 관계 유지 지출"),
    DONATION("09", "기부금", "기부·후원금"),
    DEPRECIATION("10", "감가상각비", "고정자산 상각"),
    VEHICLE("11", "차량유지비", "업무용 차량 유지비"),
    SERVICE_FEE("12", "지급수수료", "용역·서비스 이용 대가"),
    SUPPLIES("13", "소모품비", "100만원 미만 소규모 물품"),
    WELFARE("14", "복리후생비", "직원 복지·후생 지출"),
    SHIPPING("15", "운반비", "물품 배송·운송 비용"),
    ADVERTISING("16", "광고선전비", "홍보·마케팅 지출"),
    TRAVEL("17", "여비교통비", "출장·업무 이동 비용"),
    OTHER_EXPENSE("18", "기타 경비", "기타 사업 관련 지출"),

    // D. 고정자산
    ASSET_PURCHASE("19", "고정자산 매입", "100만원 이상 사업용 자산 취득"),
    ASSET_SALE("20", "고정자산 매도", "사업용 자산 처분"),

    // E. 추가 세목 — 국세청고시 제2024-19호 간편장부 기준, 1인 IT개발자 필수 항목
    COMMUNICATION("21", "통신비", "휴대폰·인터넷·서버 호스팅 요금"),
    EDUCATION("22", "교육훈련비", "온라인 강의·컨퍼런스·자격증 (소득세법 §19 필요경비)"),
    BOOKS("23", "도서인쇄비", "전문서적·기술서적·인쇄물"),

    // F. seed data 연동 — mcc_tax_rule에서 반환하는 추가 세목
    UTILITIES("24", "수도광열비", "전기·가스·수도 (별도 사무실 있는 경우만, 소득세법시행령 §55)"),
    REPAIR("25", "수선비", "사업용 장비 수리·유지보수"),
    NOT_DEDUCTIBLE("90", "경비불인정", "가사경비 — 1인 사업자 본인 식대·간식 등 (소득세법 §33①5)"),
    UNCLASSIFIED("99", "미분류", "분류 대기 — 사용자 확인 필요");

    private final String code;
    private final String name;
    private final String description;
}
