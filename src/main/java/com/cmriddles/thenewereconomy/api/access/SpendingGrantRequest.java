package com.cmriddles.thenewereconomy.api.access;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** A stable, integration-local grant key, owned source account, delegate, and bounded spending policy. */
public record SpendingGrantRequest(String key, String accountKey, String delegateIntegration, String currency,
        BigDecimal maximumPayment, BigDecimal periodBudget, long periodSeconds, Instant expiresAt) {
    public SpendingGrantRequest {
        Objects.requireNonNull(key, "key"); Objects.requireNonNull(accountKey, "accountKey");
        Objects.requireNonNull(delegateIntegration, "delegateIntegration"); Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(maximumPayment, "maximumPayment"); Objects.requireNonNull(periodBudget, "periodBudget");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
}
