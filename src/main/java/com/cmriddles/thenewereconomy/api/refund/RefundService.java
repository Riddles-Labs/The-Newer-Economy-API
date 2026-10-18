package com.cmriddles.thenewereconomy.api.refund;

import com.cmriddles.thenewereconomy.api.economy.TransactionReceipt;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Allocation refunds and disputes concerning this integration's own committed transactions. */
public interface RefundService {
    CompletionStage<Result<RefundAllowance>> remaining(UUID originalTransactionId, int creditSequence, int debitSequence);
    /** Appends funded compensating entries; concurrent refunds cannot exceed either original allocation. */
    CompletionStage<Result<TransactionReceipt>> refund(RefundRequest request);
    CompletionStage<Result<Dispute>> openDispute(UUID originalTransactionId, String key, String reason);
    CompletionStage<Result<Dispute>> resolveDispute(UUID disputeId, DisputeState state, String reason);
    CompletionStage<Result<List<Dispute>>> disputes(UUID originalTransactionId, int limit, int offset);
}
