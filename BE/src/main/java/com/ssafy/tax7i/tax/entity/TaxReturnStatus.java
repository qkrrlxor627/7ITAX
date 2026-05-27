package com.ssafy.tax7i.tax.entity;

import java.util.Map;
import java.util.Set;

public enum TaxReturnStatus {
    DRAFT,
    SUBMITTED,
    ACCEPTED,
    PAID,
    COMPLETED;

    private static final Map<TaxReturnStatus, Set<TaxReturnStatus>> TRANSITIONS = Map.of(
            DRAFT, Set.of(SUBMITTED),
            SUBMITTED, Set.of(ACCEPTED),
            ACCEPTED, Set.of(PAID),
            PAID, Set.of(COMPLETED),
            COMPLETED, Set.of()
    );

    public boolean canTransitionTo(TaxReturnStatus next) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }
}
