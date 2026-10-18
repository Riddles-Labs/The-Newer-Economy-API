package com.cmriddles.thenewereconomy.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.account.AccountService;
import com.cmriddles.thenewereconomy.api.currency.CurrencyInfo;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import com.cmriddles.thenewereconomy.api.economy.Balance;
import com.cmriddles.thenewereconomy.api.economy.AccountBalance;
import com.cmriddles.thenewereconomy.api.economy.Transaction;
import com.cmriddles.thenewereconomy.api.economy.TransactionReceipt;
import com.cmriddles.thenewereconomy.api.economy.TransactionRequest;
import com.cmriddles.thenewereconomy.api.result.CorrelationId;
import com.cmriddles.thenewereconomy.api.result.Failure;
import com.cmriddles.thenewereconomy.api.result.Result;
import com.cmriddles.thenewereconomy.api.result.ResultCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

class EconomyClientTest {
    @Test
    void shortOverloadsUseTheConfiguredCurrencyAndPreserveTheActualOperationResult() {
        RecordingClient client = new RecordingClient();
        UUID payer = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("12.50");

        assertThat(client.balance(payer)).isSameAs(client.balanceResult);
        assertThat(client.lastCall).isEqualTo(new Call("balance", payer, null, "gems", null, null));

        assertThat(client.transfer(payer, recipient, amount, "order:17"))
                .isSameAs(client.transactionResult);
        assertThat(client.lastCall).isEqualTo(new Call("transfer", payer, recipient, "gems", amount, "order:17"));

        assertThat(client.deposit(recipient, amount, "reward:18")).isSameAs(client.transactionResult);
        assertThat(client.lastCall).isEqualTo(new Call("deposit", recipient, null, "gems", amount, "reward:18"));

        assertThat(client.withdraw(payer, amount, "shop:19")).isSameAs(client.transactionResult);
        assertThat(client.lastCall).isEqualTo(new Call("withdraw", payer, null, "gems", amount, "shop:19"));
    }

    @Test
    void namedAccountConveniencesPreserveTheChosenFundingAndReceivingAccounts() {
        RecordingClient client = new RecordingClient();
        AccountRef pool = AccountRef.account(UUID.randomUUID());
        UUID playerId = UUID.randomUUID();
        AccountRef player = AccountRef.player(playerId);
        BigDecimal amount = new BigDecimal("12.50");

        assertThat(client.transfer(pool, player, amount, "pool:pay:1")).isSameAs(client.transactionResult);
        assertThat(client.lastAccountCall).isEqualTo(new AccountCall("transfer", pool, player, "gems", amount, "pool:pay:1"));

        assertThat(client.pay(pool, playerId, amount, "pool:pay:2")).isSameAs(client.transactionResult);
        assertThat(client.lastAccountCall).isEqualTo(new AccountCall("transfer", pool, player, "gems", amount, "pool:pay:2"));

        assertThat(client.pay(pool, playerId, "coins", amount, "pool:pay:3")).isSameAs(client.transactionResult);
        assertThat(client.lastAccountCall).isEqualTo(new AccountCall("transfer", pool, player, "coins", amount, "pool:pay:3"));

        assertThat(client.charge(playerId, pool, amount, "pool:charge:1")).isSameAs(client.transactionResult);
        assertThat(client.lastAccountCall).isEqualTo(new AccountCall("transfer", player, pool, "gems", amount, "pool:charge:1"));

        assertThat(client.charge(playerId, pool, "coins", amount, "pool:charge:2")).isSameAs(client.transactionResult);
        assertThat(client.lastAccountCall).isEqualTo(new AccountCall("transfer", player, pool, "coins", amount, "pool:charge:2"));

        assertThat(client.balance(pool)).isSameAs(client.accountBalanceResult);
        assertThat(client.lastAccountCall).isEqualTo(new AccountCall("balance", pool, null, "gems", null, null));

        assertThat(client.balances(playerId)).isSameAs(client.accountBalancesResult);
        assertThat(client.lastAccountCall).isEqualTo(new AccountCall("balances", player, null, null, null, null));
    }

    private record Call(String operation, UUID player, UUID recipient, String currency, BigDecimal amount, String key) { }
    private record AccountCall(String operation, AccountRef source, AccountRef destination,
            String currency, BigDecimal amount, String key) { }

    private static final class RecordingClient implements EconomyClient {
        private final Failure unavailable = new Failure(ResultCode.DEPENDENCY_UNAVAILABLE,
                "Not ready", new CorrelationId(UUID.randomUUID()));
        private final CompletionStage<Result<Balance>> balanceResult =
                CompletableFuture.completedFuture(Result.failure(unavailable));
        private final CompletionStage<Result<Transaction>> transactionResult =
                CompletableFuture.completedFuture(Result.failure(unavailable));
        private final CompletionStage<Result<AccountBalance>> accountBalanceResult =
                CompletableFuture.completedFuture(Result.failure(unavailable));
        private final CompletionStage<Result<List<AccountBalance>>> accountBalancesResult =
                CompletableFuture.completedFuture(Result.failure(unavailable));
        private Call lastCall;
        private AccountCall lastAccountCall;

        @Override
        public String integrationName() {
            return "ExamplePlugin";
        }

        @Override
        public ScopeId scope() {
            return ScopeId.network();
        }

        @Override
        public CurrencyInfo defaultCurrency() {
            return new CurrencyInfo("gems", "Gem", "Gems", "G", 2);
        }

        @Override
        public AccountService accounts() {
            throw new UnsupportedOperationException("Not used by overload routing tests");
        }

        @Override
        public CompletionStage<Result<TransactionReceipt>> post(TransactionRequest request) {
            throw new UnsupportedOperationException("Not used by overload routing tests");
        }

        @Override
        public CompletionStage<Result<Optional<TransactionReceipt>>> receipt(String idempotencyKey) {
            throw new UnsupportedOperationException("Not used by overload routing tests");
        }

        @Override
        public CompletionStage<Result<Optional<TransactionReceipt>>> receipt(UUID transactionId) {
            throw new UnsupportedOperationException("Not used by overload routing tests");
        }

        @Override
        public CompletionStage<Result<List<TransactionReceipt>>> history(AccountRef reference, int limit, int offset) {
            throw new UnsupportedOperationException("Not used by overload routing tests");
        }

        @Override
        public CompletionStage<Result<TransactionReceipt>> reverse(UUID original, String reason, String key) {
            throw new UnsupportedOperationException("Not used by overload routing tests");
        }

        @Override
        public CompletionStage<Result<AccountBalance>> balance(AccountRef reference, String currencyId) {
            lastAccountCall = new AccountCall("balance", reference, null, currencyId, null, null);
            return accountBalanceResult;
        }

        @Override
        public CompletionStage<Result<List<AccountBalance>>> balances(AccountRef reference) {
            lastAccountCall = new AccountCall("balances", reference, null, null, null, null);
            return accountBalancesResult;
        }

        @Override
        public CompletionStage<Result<Transaction>> transfer(
                AccountRef source, AccountRef destination, String currencyId, BigDecimal amount, String idempotencyKey) {
            lastAccountCall = new AccountCall("transfer", source, destination, currencyId, amount, idempotencyKey);
            return transactionResult;
        }

        @Override
        public CompletionStage<Result<Optional<Transaction>>> transaction(String idempotencyKey) {
            return CompletableFuture.completedFuture(Result.failure(unavailable));
        }

        @Override
        public CompletionStage<Result<Balance>> balance(UUID playerId, String currencyId) {
            lastCall = new Call("balance", playerId, null, currencyId, null, null);
            return balanceResult;
        }

        @Override
        public CompletionStage<Result<Transaction>> transfer(
                UUID fromPlayerId, UUID toPlayerId, String currencyId, BigDecimal amount, String idempotencyKey) {
            lastCall = new Call("transfer", fromPlayerId, toPlayerId, currencyId, amount, idempotencyKey);
            return transactionResult;
        }

        @Override
        public CompletionStage<Result<Transaction>> deposit(
                UUID playerId, String currencyId, BigDecimal amount, String idempotencyKey) {
            lastCall = new Call("deposit", playerId, null, currencyId, amount, idempotencyKey);
            return transactionResult;
        }

        @Override
        public CompletionStage<Result<Transaction>> withdraw(
                UUID playerId, String currencyId, BigDecimal amount, String idempotencyKey) {
            lastCall = new Call("withdraw", playerId, null, currencyId, amount, idempotencyKey);
            return transactionResult;
        }
    }
}
