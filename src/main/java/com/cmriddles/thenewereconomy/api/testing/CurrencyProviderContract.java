package com.cmriddles.thenewereconomy.api.testing;

import com.cmriddles.thenewereconomy.api.extension.CurrencyProvider;
import com.cmriddles.thenewereconomy.api.result.Result;
import com.cmriddles.thenewereconomy.api.result.ResultCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/** Destructive contract probes for isolated, disposable provider fixtures; never use on live player funds. */
public final class CurrencyProviderContract {
    private CurrencyProviderContract() { }

    /** A completed contract probe with readable violations. */
    public record Report(List<String> violations) {
        public Report { violations = List.copyOf(violations); }
        public boolean passed() { return violations.isEmpty(); }
    }

    /**
     * Applies a funded debit twice using the same new key, queries its recovery receipt,
     * and verifies exactly one debit. Requires BALANCE, DEBIT, and RECOVERY support,
     * an isolated pre-funded account, and no concurrent changes to that fixture.
     * A passing test leaves the fixture debited once. The five-second test deadline
     * does not cancel an external effect already in progress.
     */
    public static CompletionStage<Report> debitIdempotency(CurrencyProvider provider, CurrencyProvider.Operation operation) {
        if (operation.action() != CurrencyProvider.Capability.DEBIT || operation.amount().signum() <= 0) {
            throw new IllegalArgumentException("A positive debit operation is required");
        }
        return CompletableFuture.completedFuture(Boolean.TRUE)
                .thenCompose(ignored -> provider.balance(operation.accountKey()))
                .thenCompose(before -> {
                    BigDecimal starting = value(before).available();
                    if (starting.compareTo(operation.amount()) < 0) throw new IllegalArgumentException("Fixture must be pre-funded");
                    return provider.apply(operation).thenCompose(first -> {
                        var committed = value(first);
                        return provider.apply(operation).thenCompose(second -> provider.recover(operation.idempotencyKey())
                                .thenCompose(recovered -> provider.balance(operation.accountKey()).thenApply(after -> {
                                    List<String> violations = new ArrayList<>();
                                    if (committed.state() != CurrencyProvider.State.CONFIRMED) violations.add("First debit was not confirmed");
                                    if (!committed.operationKey().equals(operation.idempotencyKey())) violations.add("First receipt has a different operation key");
                                    if (second.isSuccess()) {
                                        if (!second.value().equals(committed)) violations.add("Duplicate request returned a different receipt");
                                    } else if (second.failure().code() != ResultCode.CONFLICT) violations.add("Duplicate request did not recover or report a conflict");
                                    var recovery = value(recovered);
                                    if (recovery.isEmpty() || !recovery.orElseThrow().equals(committed)) violations.add("Recovery did not return the committed receipt");
                                    if (value(after).available().compareTo(starting.subtract(operation.amount())) != 0) violations.add("Balance does not reflect exactly one debit");
                                    return new Report(violations);
                                })));
                    });
                }).toCompletableFuture().orTimeout(5, TimeUnit.SECONDS)
                .handle((report, error) -> error == null ? report : new Report(List.of("Provider probe failed or exceeded its deadline")))
                .minimalCompletionStage();
    }

    private static <T> T value(Result<T> result) {
        if (!result.isSuccess()) throw new IllegalStateException("Provider operation failed: " + result.failure().code());
        return result.value();
    }
}
