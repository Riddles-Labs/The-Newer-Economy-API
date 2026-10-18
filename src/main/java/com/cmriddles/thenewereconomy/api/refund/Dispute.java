package com.cmriddles.thenewereconomy.api.refund;

import java.time.Instant;
import java.util.UUID;

/** Persisted dispute with its original reason and latest review explanation. */
public record Dispute(UUID id, UUID originalTransactionId, String key, DisputeState state, String reason,
        String resolution, Instant openedAt, Instant updatedAt) { }
