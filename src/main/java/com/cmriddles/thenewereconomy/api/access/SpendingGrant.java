package com.cmriddles.thenewereconomy.api.access;

import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Persisted opt-in spending policy. It is not a sandbox over trusted installed plugins. */
public record SpendingGrant(
        UUID id,
        String key,
        UUID accountId,
        String ownerIntegration,
        String delegateIntegration,
        String currency,
        ScopeId scope,
        BigDecimal maximumPayment,
        BigDecimal periodBudget,
        long periodSeconds,
        Instant periodStartedAt,
        BigDecimal spent,
        Instant expiresAt,
        boolean revoked) {}
