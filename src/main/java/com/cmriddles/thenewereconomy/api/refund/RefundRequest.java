package com.cmriddles.thenewereconomy.api.refund;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Refunds part of an original credit to an explicitly selected original debit allocation. */
public record RefundRequest(
        UUID originalTransactionId,
        int creditSequence,
        int debitSequence,
        BigDecimal amount,
        String reason,
        String idempotencyKey) {
    public RefundRequest {
        Objects.requireNonNull(originalTransactionId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(idempotencyKey);
    }
}
