package com.cmriddles.thenewereconomy.api.schedule;

import com.cmriddles.thenewereconomy.api.result.Result;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
/**
 * Durable funded one-off/recurring payments. Insufficient funds pause the obligation until explicit resume.
 * No funds are reserved by creation. Each due occurrence requires funding when its transaction commits.
 * Overdue recurring occurrences catch up in bounded worker batches rather than being silently skipped.
 */
public interface ScheduleService {
 /** Creates or returns the same saved terms; reusing a key with changed terms fails. */
 CompletionStage<Result<ScheduledPayment>> create(ScheduleRequest request);
 CompletionStage<Result<Optional<ScheduledPayment>>> find(UUID id);
 CompletionStage<Result<Optional<ScheduledPayment>>> find(String key);
 /** Reads most recent occurrences first; limit is 1..100 and offset is nonnegative. */
 CompletionStage<Result<List<ScheduleOccurrence>>> occurrences(UUID id, int limit, int offset);
 /** Stops unpaid future occurrences; never reverses payments that already committed. */
 CompletionStage<Result<ScheduledPayment>> cancel(UUID id);
 /** Retries the saved pending occurrence key after funding/policy recovery; cancelled/completed schedules stay terminal. */
 CompletionStage<Result<ScheduledPayment>> resume(UUID id);
}
