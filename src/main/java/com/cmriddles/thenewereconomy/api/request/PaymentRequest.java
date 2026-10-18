package com.cmriddles.thenewereconomy.api.request;

import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Persisted invoice snapshot; status is rechecked atomically when money moves. */
public record PaymentRequest(
        UUID id,
        AccountRef requester,
        AccountRef payer,
        String currency,
        ScopeId scope,
        BigDecimal amount,
        Status status,
        String message,
        Optional<Instant> expiresAt,
        Instant createdAt) {
    public enum Status {
        PENDING,
        ACCEPTED,
        DENIED,
        CANCELLED,
        EXPIRED
    }
}
