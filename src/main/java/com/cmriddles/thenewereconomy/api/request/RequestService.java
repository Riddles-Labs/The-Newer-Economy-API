package com.cmriddles.thenewereconomy.api.request;

import com.cmriddles.thenewereconomy.api.economy.TransactionReceipt;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Durable invoices. Your plugin must authenticate the explicit payer before accepting or denying.
 */
public interface RequestService {
    /**
     * Creates or recovers this integration's request using its stable key; never charges the payer.
     */
    CompletionStage<Result<PaymentRequest>> create(RequestDraft draft);

    /** Finds only a request created by this integration. */
    CompletionStage<Result<Optional<PaymentRequest>>> find(UUID requestId);

    /**
     * Lists this payer's pending requests in the configured scope, across integrations; limit
     * 1..100.
     */
    CompletionStage<Result<List<PaymentRequest>>> pending(UUID payer, int limit, int offset);

    /** Explicit authenticated payer consent, with status and transfer committed atomically. */
    CompletionStage<Result<TransactionReceipt>> accept(
            UUID requestId, UUID payer, String idempotencyKey);

    CompletionStage<Result<PaymentRequest>> deny(UUID requestId, UUID payer);

    /** Only this integration's own pending request can be cancelled. */
    CompletionStage<Result<PaymentRequest>> cancel(UUID requestId);
}
