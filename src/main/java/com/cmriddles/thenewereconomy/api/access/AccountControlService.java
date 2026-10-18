package com.cmriddles.thenewereconomy.api.access;

import com.cmriddles.thenewereconomy.api.account.AccountState;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/** Audited metadata and lifecycle changes for accounts owned by this integration. */
public interface AccountControlService {
    CompletionStage<Result<AccountProfile>> profile(String accountKey);

    /**
     * Replaces up to 16 metadata fields; keys have at most 64 and values at most 512 printable
     * characters.
     */
    CompletionStage<Result<AccountProfile>> metadata(
            String accountKey, Map<String, String> metadata, String reason);

    /** Closed accounts cannot reopen. Closing requires zero balances and no active obligations. */
    CompletionStage<Result<AccountProfile>> state(
            String accountKey, AccountState state, String reason);
}
