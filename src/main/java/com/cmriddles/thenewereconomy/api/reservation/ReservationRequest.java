package com.cmriddles.thenewereconomy.api.reservation;

import com.cmriddles.thenewereconomy.api.economy.Payment;
import com.cmriddles.thenewereconomy.api.transaction.IdempotencyKey;
import java.time.Instant;
import java.util.Objects;
/** A stable-key hold for one funded payment; capture pays its fixed destination. */
public record ReservationRequest(String key, Payment payment, Instant expiresAt) {
 public ReservationRequest { key = new IdempotencyKey(key).value(); Objects.requireNonNull(payment); Objects.requireNonNull(expiresAt); }
}
