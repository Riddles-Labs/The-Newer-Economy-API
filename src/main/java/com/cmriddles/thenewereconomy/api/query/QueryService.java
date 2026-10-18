package com.cmriddles.thenewereconomy.api.query;

import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.economy.AccountBalance;
import com.cmriddles.thenewereconomy.api.economy.TransactionReceipt;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.util.List;
import java.util.concurrent.CompletionStage;

/** Bounded server-trusted reads. Authorize players before exposing account information. */
public interface QueryService {
    /**
     * At most 100 account references and 100 currencies; the result is bounded to 1000 balances.
     */
    CompletionStage<Result<List<AccountBalance>>> balances(
            List<AccountRef> accounts, List<String> currencies);

    CompletionStage<Result<List<TransactionReceipt>>> history(HistoryQuery query);

    /** Currency ID, or "all" for average percentile rank; limit 1..100. */
    CompletionStage<Result<Leaderboard>> leaderboard(String selector, int limit, int offset);
}
