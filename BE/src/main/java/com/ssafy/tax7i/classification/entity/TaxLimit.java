package com.ssafy.tax7i.classification.entity;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 세목별 한도·알림 룰 (seed.xlsx 한도_알림_v3 시트 기준)
 *
 * 하나의 세목에 여러 한도 유형이 존재할 수 있다.
 * 예: 접대비 → 연간기본한도(1,200만), 건당증빙기준(3만), 경조사비한도(20만)
 */
@Entity
@Table(name = "tax_limit", indexes = {
        @Index(name = "idx_tax_limit_category", columnList = "taxCategory"),
        @Index(name = "idx_tax_limit_type", columnList = "limitType")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxLimit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 세목 (접대비, 차량유지비, 감가상각비, 통신비 등) */
    @Column(nullable = false, length = 50)
    private String taxCategory;

    /** 한도 유형 (연간기본한도, 건당증빙기준, 경조사비한도, 연간총한도, 사업용비율, 없음 등) */
    @Column(nullable = false, length = 50)
    private String limitType;

    /** 한도 금액(원). 0이면 금액 한도 없음 */
    @Column(nullable = false)
    private Long limitAmount = 0L;

    /** 단위 (연, 건, %, 4년 등) */
    @Column(nullable = false, length = 20)
    private String unit;

    /** 세법 근거 */
    @Column(length = 200)
    private String legalBasis;

    /** 7iTAX 알림 메시지 템플릿. {pct}, {used}, {limit}, {ratio} 등 플레이스홀더 사용 */
    @Column(length = 500)
    private String alertMessage;

}
