package com.cmriddles.thenewereconomy.api.extension;

import com.cmriddles.thenewereconomy.api.result.Result;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * Policies apply to this integration's native transactions. Registrations do not survive plugin
 * shutdown.
 */
public interface ExtensionService {
    Registration policy(String stableKey, TransactionPolicy policy);

    ProviderRegistration provider(
            String key,
            String displayName,
            String symbol,
            int scale,
            Set<CurrencyProvider.Capability> capabilities,
            CurrencyProvider provider);

    List<CurrencyProvider.Info> providers();

    CompletionStage<Result<CurrencyProvider.Balance>> balance(String providerId, String accountKey);

    /**
     * Persists intent before dispatch. An uncertain response requires recover(), never a new key.
     */
    CompletionStage<Result<CurrencyProvider.Receipt>> execute(
            String providerId, CurrencyProvider.Operation operation);

    CompletionStage<Result<CurrencyProvider.Receipt>> recover(
            String providerId, String idempotencyKey);
}
