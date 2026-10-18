package com.cmriddles.thenewereconomy.api.exchange;
import com.cmriddles.thenewereconomy.api.economy.TransactionReceipt;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
/** Funded currency conversion with an expiring, configuration-bound confirmation. */
public interface ExchangeService {
    CompletionStage<Result<ExchangeQuote>> quote(UUID player, String sourceCurrency, String targetCurrency, BigDecimal sourceAmount);
    /** Confirms a quote issued by this integration to this player; changed configuration requires a new quote. */
    CompletionStage<Result<TransactionReceipt>> execute(UUID player, UUID quoteId, String idempotencyKey);
}
