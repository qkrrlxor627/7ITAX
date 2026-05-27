package com.ssafy.tax7i.classification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "merchant", indexes = {
        @Index(name = "idx_merchant_mcc", columnList = "mcc"),
        @Index(name = "idx_merchant_name", columnList = "merchantName")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String merchantName;

    @Column(nullable = false, length = 10)
    private String mcc;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String exampleNames;

    @Column(nullable = false)
    private Boolean isDomestic = true;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
