package com.ssafy.tax7i.taxcalendar.entity;

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
@Table(name = "tax_deadlines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tax_deadline_year_name",
                columnNames = {"\"year\"", "name"}),
        indexes = @Index(name = "idx_tax_deadline_year", columnList = "\"year\""))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxDeadline extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "\"year\"", nullable = false)
    private int year;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "deadline_month", nullable = false)
    private int deadlineMonth;

    @Column(name = "deadline_day", nullable = false)
    private int deadlineDay;

    @Builder
    public TaxDeadline(int year, String name, String description, int deadlineMonth, int deadlineDay) {
        this.year = year;
        this.name = name;
        this.description = description;
        this.deadlineMonth = deadlineMonth;
        this.deadlineDay = deadlineDay;
    }
}
