package com.cmriddles.thenewereconomy.api.workflow;

import com.cmriddles.thenewereconomy.api.reservation.ReservationRequest;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
/**
 * Durable multistep coordination, not a distributed transaction with an inventory or external database.
 * Begin a step before delivery and acknowledge its saved token only after durable, idempotent delivery.
 * A pending/unknown step is quarantined for explicit reconciliation; TNE never replays Java callbacks.
 * Once any step begins, uncertain or acknowledged external delivery protects the hold from automatic
 * expiry. Funds remain reserved until explicit reconciliation and capture, or safe cancellation
 * before any acknowledged delivery. This is deliberate protection against free, uncharged delivery.
 */
public interface WorkflowService {
 CompletionStage<Result<Workflow>> create(String key, ReservationRequest reservation, List<String> steps);
 CompletionStage<Result<Optional<Workflow>>> find(UUID id);
 CompletionStage<Result<Optional<Workflow>>> find(String key);
 /** Claims the current step and durably saves a new delivery token before the caller performs external work. */
 CompletionStage<Result<Workflow>> beginStep(UUID id);
 /** Advances exactly the pending step matching this token; call only after durable external delivery. */
 CompletionStage<Result<Workflow>> acknowledgeStep(UUID id, UUID deliveryToken);
 /** Marks uncertain delivery for reconciliation without paying, refunding, or repeating it. */
 CompletionStage<Result<Workflow>> quarantine(UUID id, String reason);
 /** Explicit caller assertion after external reconciliation: delivered advances; otherwise retry is allowed. */
 CompletionStage<Result<Workflow>> recover(UUID id, boolean delivered, String reason);
 /** Captures the reservation only after every step has been explicitly acknowledged. */
 CompletionStage<Result<Workflow>> capture(UUID id);
 /** Cancels and returns funds only when no step has been acknowledged and no delivery is pending or unknown. */
 CompletionStage<Result<Workflow>> cancel(UUID id);
}
