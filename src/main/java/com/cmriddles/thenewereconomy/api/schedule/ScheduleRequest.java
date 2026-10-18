package com.cmriddles.thenewereconomy.api.schedule;

import com.cmriddles.thenewereconomy.api.economy.Payment;
import com.cmriddles.thenewereconomy.api.result.ValidationException;
import com.cmriddles.thenewereconomy.api.transaction.IdempotencyKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Durable funded obligation. ZERO interval means one payment; positive intervals are at least one
 * second.
 */
public record ScheduleRequest(
        String key, Payment payment, Instant firstDue, Duration interval, int maxOccurrences) {
    public ScheduleRequest {
        key = new IdempotencyKey(key).value();
        Objects.requireNonNull(payment);
        Objects.requireNonNull(firstDue);
        Objects.requireNonNull(interval);
        if (interval.isNegative()
                || (!interval.isZero() && interval.compareTo(Duration.ofSeconds(1)) < 0)
                || maxOccurrences < 1
                || maxOccurrences > 1000000
                || (interval.isZero() && maxOccurrences != 1))
            throw new ValidationException(
                    "Use 1..1000000 occurrences and a zero one-off or >=1 second recurring interval");
        Math.addExact(
                firstDue.toEpochMilli(),
                Math.multiplyExact(interval.toMillis(), (long) maxOccurrences - 1));
    }
}
