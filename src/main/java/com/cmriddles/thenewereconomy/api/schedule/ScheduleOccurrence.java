package com.cmriddles.thenewereconomy.api.schedule;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
/** One durable attempt with an immutable operation key; resuming retries this same key. */
public record ScheduleOccurrence(UUID scheduleId, int number, Instant dueAt, String idempotencyKey,
 OccurrenceStatus status, Optional<UUID> transactionId, String message) { }
