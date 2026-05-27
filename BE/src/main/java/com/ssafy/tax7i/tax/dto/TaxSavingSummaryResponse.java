package com.ssafy.tax7i.tax.dto;

public record TaxSavingSummaryResponse(
        LimitTracking limitTracking,
        TaxReductions taxReductions
) {
    public record LimitTracking(
            LimitItem entertainment,
            LimitItem vehicle,
            YellowUmbrellaItem yellowUmbrella
    ) {}

    public record LimitItem(
            long used,
            long limit,
            double ratio
    ) {}

    public record YellowUmbrellaItem(
            long paid,
            long deductionLimit,
            long estimatedSaving
    ) {}

    public record TaxReductions(
            SmeReductionItem smeSpecialReduction,
            BookkeepingCreditItem bookkeepingCredit
    ) {}

    public record SmeReductionItem(
            boolean eligible,
            double reductionRate,
            long estimatedAmount
    ) {}

    public record BookkeepingCreditItem(
            boolean eligible,
            double creditRate,
            long estimatedAmount
    ) {}
}
