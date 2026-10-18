package com.cmriddles.thenewereconomy.api.schedule;

import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import com.cmriddles.thenewereconomy.api.economy.Payment;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
/** Saved obligation; overdue recurring occurrences catch up in bounded worker batches, without issuing money. */
public record ScheduledPayment(UUID id, String key, Payment payment, ScopeId scope, Instant firstDue,
 Instant nextDue, Duration interval, int maxOccurrences, int completedOccurrences, ScheduleStatus status) { }
