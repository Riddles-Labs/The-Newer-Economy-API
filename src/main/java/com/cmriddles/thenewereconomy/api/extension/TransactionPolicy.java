package com.cmriddles.thenewereconomy.api.extension;

import com.cmriddles.thenewereconomy.api.economy.Payment;
import com.cmriddles.thenewereconomy.api.economy.TransactionRequest;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Bounded preflight extension. No game-thread access; durable limits must use ledger-enforced
 * budgets.
 */
@FunctionalInterface
public interface TransactionPolicy {
    CompletionStage<Decision> evaluate(Context context);

    record Context(
            String integrationName, TransactionRequest request, boolean lifecycleOperation) {}

    /**
     * Extra fee/tax allocations are supported on ordinary post operations; lifecycle plans stay
     * exact.
     */
    record Decision(boolean permitted, String reason, List<Payment> allocations) {
        public Decision {
            reason = java.util.Objects.requireNonNull(reason);
            allocations = List.copyOf(allocations);
        }

        public static Decision allow() {
            return new Decision(true, "", List.of());
        }

        public static Decision deny(String reason) {
            return new Decision(false, reason, List.of());
        }
    }
}
