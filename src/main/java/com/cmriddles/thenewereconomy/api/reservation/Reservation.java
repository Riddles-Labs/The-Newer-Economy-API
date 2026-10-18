package com.cmriddles.thenewereconomy.api.reservation;

import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import com.cmriddles.thenewereconomy.api.economy.Payment;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Persisted hold. ACTIVE money is isolated in escrow and cannot be spent normally. */
public record Reservation(
        UUID id,
        String key,
        Payment payment,
        AccountRef escrow,
        ScopeId scope,
        ReservationStatus status,
        Instant expiresAt,
        Optional<UUID> transactionId) {}
