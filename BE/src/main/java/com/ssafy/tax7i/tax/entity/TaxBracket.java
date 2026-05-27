package com.ssafy.tax7i.tax.entity;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tax_brackets", indexes = {
        @Index(name = "idx_tb_year", columnList = "\"year\"")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxBracket extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "\"year\"", nullable = false)
    private int year;

    @Column(nullable = false)
    private long bracketMin;

    @Column(nullable = false)
    private long bracketMax;

    @Column(nullable = false)
    private double rate;

    @Column(nullable = false)
    private long progressiveDeduction;

    @Builder
    public TaxBracket(int year, long bracketMin, long bracketMax, double rate, long progressiveDeduction) {
        this.year = year;
        this.bracketMin = bracketMin;
        this.bracketMax = bracketMax;
        this.rate = rate;
        this.progressiveDeduction = progressiveDeduction;
    }
}
