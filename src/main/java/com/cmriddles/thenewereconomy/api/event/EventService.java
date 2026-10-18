package com.cmriddles.thenewereconomy.api.event;
import com.cmriddles.thenewereconomy.api.extension.Registration;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Consumer;
/** Durable polling and process-local notifications. Save your own cursor for restart recovery. */
public interface EventService {
 CompletionStage<Result<EventPage>> read(long afterSequence, int limit, EventFilter filter);
 /** Cursor advances only after the callback completes successfully. Failed batches are redelivered. */
 Registration subscribe(long afterSequence, EventFilter filter, Function<EventPage,CompletionStage<Void>> listener);
 /** Process-local readiness notifications, including immediate current status. */
 Registration availability(Consumer<Boolean> listener);
}
