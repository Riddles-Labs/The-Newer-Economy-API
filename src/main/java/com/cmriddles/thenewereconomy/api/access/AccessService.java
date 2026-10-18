package com.cmriddles.thenewereconomy.api.access;

import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.economy.TransactionReceipt;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Explicit delegated payments enforce grant limits atomically; ordinary trusted transfers remain
 * available.
 */
public interface AccessService {
    /**
     * Creates a policy once; repeating its key returns the original policy without resetting its
     * budget.
     */
    CompletionStage<Result<SpendingGrant>> grant(SpendingGrantRequest request);

    CompletionStage<Result<Optional<SpendingGrant>>> find(UUID grantId);

    CompletionStage<Result<List<SpendingGrant>>> list(String accountKey, int limit, int offset);

    CompletionStage<Result<SpendingGrant>> revoke(UUID grantId, String reason);

    /**
     * Only the named delegate can use this path; expiry and per-payment/period limits are
     * commit-time checks.
     */
    CompletionStage<Result<TransactionReceipt>> pay(
            UUID grantId, AccountRef destination, BigDecimal amount, String idempotencyKey);
}
