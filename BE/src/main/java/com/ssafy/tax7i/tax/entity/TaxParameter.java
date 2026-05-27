package com.ssafy.tax7i.tax.entity;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tax_parameters",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tax_param_year_cat_key",
                columnNames = {"\"year\"", "category", "param_key"}),
        indexes = @Index(name = "idx_tax_param_year_cat", columnList = "\"year\", category"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxParameter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "\"year\"", nullable = false)
    private int year;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "param_key", nullable = false, length = 100)
    private String paramKey;

    @Column(name = "param_value", nullable = false, length = 200)
    private String paramValue;

    @Column(length = 500)
    private String description;

    @Column(name = "legal_basis", length = 200)
    private String legalBasis;

    @Builder
    public TaxParameter(int year, String category, String paramKey, String paramValue,
                         String description, String legalBasis) {
        this.year = year;
        this.category = category;
        this.paramKey = paramKey;
        this.paramValue = paramValue;
        this.description = description;
        this.legalBasis = legalBasis;
    }
}
