package com.cmriddles.thenewereconomy.api.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.cmriddles.thenewereconomy.api.extension.CurrencyProvider;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

final class CurrencyProviderContractTest {
    @Test void probeAcceptsIdempotentProviderAndFindsDoubleDebits() {
        var operation = new CurrencyProvider.Operation(CurrencyProvider.Capability.DEBIT, "fixture", Optional.empty(),
                BigDecimal.ONE, "probe:1", Optional.empty());
        assertThat(CurrencyProviderContract.debitIdempotency(provider(true), operation).toCompletableFuture().join().passed()).isTrue();
        assertThat(CurrencyProviderContract.debitIdempotency(provider(false), operation).toCompletableFuture().join().violations())
                .contains("Balance does not reflect exactly one debit");
    }

    private static CurrencyProvider provider(boolean idempotent) {
        return new CurrencyProvider() {
            private BigDecimal balance = BigDecimal.TEN;
            private Receipt receipt;
            @Override public CompletionStage<Result<Balance>> balance(String key) {
                return CompletableFuture.completedFuture(Result.success(new Balance(key, balance, BigDecimal.ZERO)));
            }
            @Override public CompletionStage<Result<Receipt>> apply(Operation operation) {
                if (receipt == null || !idempotent) balance = balance.subtract(operation.amount());
                receipt = new Receipt(operation.idempotencyKey(), State.CONFIRMED, "reference:1", "");
                return CompletableFuture.completedFuture(Result.success(receipt));
            }
            @Override public CompletionStage<Result<Optional<Receipt>>> recover(String key) {
                return CompletableFuture.completedFuture(Result.success(Optional.ofNullable(receipt)));
            }
        };
    }
}
