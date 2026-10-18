package com.cmriddles.thenewereconomy.api;

import com.cmriddles.thenewereconomy.api.account.AccountService;
import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.currency.CurrencyInfo;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import com.cmriddles.thenewereconomy.api.economy.Balance;
import com.cmriddles.thenewereconomy.api.economy.AccountBalance;
import com.cmriddles.thenewereconomy.api.economy.Transaction;
import com.cmriddles.thenewereconomy.api.economy.TransactionReceipt;
import com.cmriddles.thenewereconomy.api.economy.TransactionRequest;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Asynchronous account management and virtual-money operations attributed to one plugin integration.
 *
 * <p>Methods return promptly and run storage work away from the calling thread. Check
 * {@link Result#isSuccess()} before reading {@link Result#value()}; on failure inspect
 * {@link Result#failure()}. Invalid operation arguments return a failed result with
 * {@code VALIDATION_ERROR} when the runtime is ready. Unavailable storage returns
 * {@code DEPENDENCY_UNAVAILABLE}.
 *
 * <p>Callbacks are not guaranteed to run on a game thread. On Paper/Folia, schedule player or world
 * changes on the appropriate player/entity/region scheduler. Never block a game thread with
 * {@code join()} or {@code get()}.
 *
 * <p>Use {@code new BigDecimal("12.50")}, not a floating-point amount. Mutations require a positive
 * amount representable at the currency's configured precision; TNE does not silently round it.
 * Currency IDs are case-insensitive identifiers, not display names.
 *
 * <p>Every money mutation requires your own stable idempotency key for that one business operation, such
 * as {@code "order:123:payment"}. Keys are 1 to 128 ASCII letters, digits, dots, underscores,
 * colons or hyphens. Persist the key with your own business record before calling TNE.
 * Reuse it when retrying that same operation; use a new key for a different operation. A committed
 * key is never applied twice. A repeated request can return {@code CONFLICT}, or another failure
 * if current-state checks, validation, or concurrency prevent it first. This is not a successful
 * receipt replay. Cancellation or losing a response does not prove the transaction was rolled
 * back. Use {@link #transaction(String)} to recover its receipt and reconcile uncertain outcomes
 * before delivering goods. Direct ledger mutations share this integration's key namespace,
 * including different currencies and mutation types. Lifecycle services document their own
 * persistent object keys and generated ledger keys; external provider intents use a separate
 * operation namespace. Internal account and ledger key prefixes are reserved by those services.
 *
 * <p>This is a trusted server-plugin API. It does not test the caller's player permissions. Your
 * plugin must authorize its own commands and business actions. Ledger restrictions and currency
 * rules still apply. All operations use the configured scope returned by {@link #scope()}.
 */
public interface EconomyClient {
    /** @return the stable declared integration name used for transaction attribution */
    String integrationName();

    /** @return the configured scope used by every operation on this client */
    ScopeId scope();

    /** @return immutable metadata for the default currency used by the short overloads */
    CurrencyInfo defaultCurrency();

    /**
     * Returns asynchronous creation, lookup, listing, and naming operations for accounts.
     *
     * @return the account service attributed to this integration
     */
    AccountService accounts();

    /** @return the reservations service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.reservation.ReservationService reservations() { throw new UnsupportedOperationException("Provider does not expose reservations"); }

    /** @return the workflows service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.workflow.WorkflowService workflows() { throw new UnsupportedOperationException("Provider does not expose workflows"); }

    /** @return the schedules service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.schedule.ScheduleService schedules() { throw new UnsupportedOperationException("Provider does not expose schedules"); }

    /** @return the controls service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.access.AccountControlService controls() { throw new UnsupportedOperationException("Provider does not expose controls"); }

    /** @return the access service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.access.AccessService access() { throw new UnsupportedOperationException("Provider does not expose access"); }

    /** @return the refunds service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.refund.RefundService refunds() { throw new UnsupportedOperationException("Provider does not expose refunds"); }

    /** @return the requests service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.request.RequestService requests() { throw new UnsupportedOperationException("Provider does not expose requests"); }

    /** @return the exchange service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.exchange.ExchangeService exchange() { throw new UnsupportedOperationException("Provider does not expose exchange"); }

    /** @return the notes service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.note.NoteService notes() { throw new UnsupportedOperationException("Provider does not expose notes"); }

    /** @return the queries service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.query.QueryService queries() { throw new UnsupportedOperationException("Provider does not expose queries"); }

    /** @return the presentation service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.presentation.PresentationService presentation() { throw new UnsupportedOperationException("Provider does not expose presentation"); }

    /** @return the extensions service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.extension.ExtensionService extensions() { throw new UnsupportedOperationException("Provider does not expose extensions"); }

    /** @return the events service; inspect provider capabilities before using optional extensions */
    default com.cmriddles.thenewereconomy.api.event.EventService events() { throw new UnsupportedOperationException("Provider does not expose events"); }



    /**
     * Commits up to 100 funded payment instructions as one atomic transaction.
     *
     * <p>Use this for split payments, bulk payments, or several account movements that must commit
     * together. The request supplies one shared idempotency key and optional business metadata.
     * TNE checks each account, currency, and precision. The net debit for each account, currency,
     * and scope must be funded; incoming payments within the same atomic request can fund its
     * outgoing payments. Instruction order is not a series of independent balance checks. This
     * does not issue new money. Receipt entries show every signed ledger change. Your external
     * delivery or database changes remain outside this atomic boundary.
     *
     * @param request immutable payments, saved operation key, and optional caller metadata
     * @return a complete receipt after all payments commit, or a failed result with no partial commit
     */
    CompletionStage<Result<TransactionReceipt>> post(TransactionRequest request);

    /**
     * Finds a complete committed receipt in this integration's namespace using its saved operation key.
     * This supports simple transfers and multi-payment transactions alike.
     *
     * <p>An empty successful optional means no committed receipt was visible at that instant. The
     * original request can still be in progress; keep the same durable business record and key.
     *
     * @param idempotencyKey this integration's original transaction key
     * @return the complete receipt when present; a successful empty optional otherwise
     */
    CompletionStage<Result<Optional<TransactionReceipt>>> receipt(String idempotencyKey);

    /**
     * Finds a complete committed receipt by UUID, restricted to this integration's own transactions.
     *
     * @param transactionId persistent transaction UUID
     * @return the complete receipt when visible in this integration's namespace, or an empty optional
     */
    CompletionStage<Result<Optional<TransactionReceipt>>> receipt(UUID transactionId);

    /**
     * Lists committed transactions touching a known account, including transactions from other origins.
     *
     * <p>This is trusted integration access to account auditing. Your plugin must authorize any
     * player-facing command that exposes these details. Entries are complete transaction receipts,
     * including the other participating accounts. Pagination reads current ledger history, so new
     * transactions can change page boundaries between requests.
     *
     * @param reference explicit player or known ledger account reference
     * @param limit maximum number of transactions to return, from 1 through 100
     * @param offset zero-based number of transactions to skip; must be nonnegative
     * @return an immutable page of receipts touching this account
     */
    CompletionStage<Result<List<TransactionReceipt>>> history(AccountRef reference, int limit, int offset);

    /**
     * Posts one exact, full reversal of an unreversed transaction created by this integration.
     *
     * <p>This appends compensating entries; it does not edit or delete the original transaction.
     * The accounts that must pay the reversal must still be eligible and funded. A transaction
     * cannot be reversed twice. Save a new stable idempotency key for the reversal itself, and
     * keep a readable reason for auditing. This does not undo external rewards or inventory changes.
     *
     * @param originalTransactionId UUID of this integration's original committed transaction
     * @param reason readable explanation for the reversal
     * @param idempotencyKey new stable key identifying this reversal operation
     * @return the committed compensating transaction receipt, or a failed result
     */
    CompletionStage<Result<TransactionReceipt>> reverse(
            UUID originalTransactionId, String reason, String idempotencyKey);

    /**
     * Reads a player, plugin, or system account's balance in the default currency.
     *
     * @param reference explicit player or existing ledger account reference
     * @return the resolved account and balance, completed asynchronously
     */
    default CompletionStage<Result<AccountBalance>> balance(AccountRef reference) {
        return balance(reference, defaultCurrency().id());
    }

    /**
     * Reads a player, plugin, or system account's balance in a specified virtual currency.
     *
     * @param reference explicit player or existing ledger account reference
     * @param currencyId configured virtual currency ID
     * @return the resolved account and balance, completed asynchronously
     */
    CompletionStage<Result<AccountBalance>> balance(AccountRef reference, String currencyId);

    /**
     * Reads an account's balances for all configured virtual currencies usable in this server's scope.
     * The returned values are snapshots, not a reservation or an atomic view across currencies.
     *
     * @param reference explicit player or existing ledger account reference
     * @return an immutable list of the account's currency balances
     */
    CompletionStage<Result<List<AccountBalance>>> balances(AccountRef reference);

    /**
     * Reads a player's balances for all configured virtual currencies in this server's scope.
     *
     * @param playerId Minecraft player's UUID
     * @return an immutable list of resolved account balances
     */
    default CompletionStage<Result<List<AccountBalance>>> balances(UUID playerId) {
        return balances(playerId == null ? null : AccountRef.player(playerId));
    }

    /**
     * Moves default-currency money atomically between explicit player, plugin, or system accounts.
     *
     * @param source account paying the amount
     * @param destination account receiving the amount
     * @param amount positive exact decimal amount
     * @param idempotencyKey stable key unique to this money operation
     * @return the committed transaction receipt, or a failed result
     */
    default CompletionStage<Result<Transaction>> transfer(
            AccountRef source, AccountRef destination, BigDecimal amount, String idempotencyKey) {
        return transfer(source, destination, defaultCurrency().id(), amount, idempotencyKey);
    }

    /**
     * Moves money atomically between explicit player, plugin, or system accounts in one currency.
     *
     * <p>Both accounts must be known and eligible for the operation. The source must be funded;
     * selecting a system account does not bypass supply, debit, or account restrictions. This method
     * never creates money. Installed plugins are trusted; your plugin must authorize who can request
     * transfers involving owner-provided account UUIDs.
     *
     * @param source account paying the amount
     * @param destination account receiving the amount
     * @param currencyId configured virtual currency ID
     * @param amount positive exact decimal amount
     * @param idempotencyKey stable key unique to this money operation
     * @return the committed transaction receipt, or a failed result
     */
    CompletionStage<Result<Transaction>> transfer(
            AccountRef source, AccountRef destination, String currencyId, BigDecimal amount, String idempotencyKey);

    /**
     * Pays a player from an explicitly selected source account in the default currency.
     *
     * @param source funded source account
     * @param playerId player receiving the payment
     * @param amount positive exact decimal amount
     * @param idempotencyKey stable key unique to this money operation
     * @return the committed transaction receipt, or a failed result
     */
    default CompletionStage<Result<Transaction>> pay(
            AccountRef source, UUID playerId, BigDecimal amount, String idempotencyKey) {
        return pay(source, playerId, defaultCurrency().id(), amount, idempotencyKey);
    }

    /**
     * Pays a player from an explicitly selected source account in one virtual currency.
     * Unlike {@link #deposit(UUID, String, BigDecimal, String)}, this chooses the funding account.
     *
     * @param source funded source account
     * @param playerId player receiving the payment
     * @param currencyId configured virtual currency ID
     * @param amount positive exact decimal amount
     * @param idempotencyKey stable key unique to this money operation
     * @return the committed transaction receipt, or a failed result
     */
    default CompletionStage<Result<Transaction>> pay(
            AccountRef source, UUID playerId, String currencyId, BigDecimal amount, String idempotencyKey) {
        return transfer(source, playerId == null ? null : AccountRef.player(playerId), currencyId, amount, idempotencyKey);
    }

    /**
     * Charges a player into an explicitly selected destination account in the default currency.
     *
     * @param playerId player paying the amount
     * @param destination account receiving the payment
     * @param amount positive exact decimal amount
     * @param idempotencyKey stable key unique to this money operation
     * @return the committed transaction receipt, or a failed result
     */
    default CompletionStage<Result<Transaction>> charge(
            UUID playerId, AccountRef destination, BigDecimal amount, String idempotencyKey) {
        return charge(playerId, destination, defaultCurrency().id(), amount, idempotencyKey);
    }

    /**
     * Charges a player into an explicitly selected destination account in one virtual currency.
     * Unlike {@link #withdraw(UUID, String, BigDecimal, String)}, this chooses the receiving account.
     *
     * @param playerId player paying the amount
     * @param destination account receiving the payment
     * @param currencyId configured virtual currency ID
     * @param amount positive exact decimal amount
     * @param idempotencyKey stable key unique to this money operation
     * @return the committed transaction receipt, or a failed result
     */
    default CompletionStage<Result<Transaction>> charge(
            UUID playerId, AccountRef destination, String currencyId, BigDecimal amount, String idempotencyKey) {
        return transfer(playerId == null ? null : AccountRef.player(playerId), destination, currencyId, amount, idempotencyKey);
    }

    /**
     * Finds a committed operation using the idempotency key saved by your integration.
     *
     * <p>This reads only this integration's key namespace. A successful result with an empty
     * optional means no committed receipt was visible at the time of the read. The original
     * operation may still be in progress, so an empty result is not proof that it failed. Keep
     * your saved business record and key; never replace that key merely because a response was lost.
     * The receipt identifies the transfer amount, currency, and scope; it is not a current balance.
     * Multi-payment transactions require {@link #receipt(String)} instead; this short receipt
     * lookup returns {@code VALIDATION_ERROR} when the committed transaction is not a single two-entry payment.
     *
     * @param idempotencyKey the stable key originally submitted for this integration's mutation
     * @return an asynchronous result containing the committed receipt, or an empty optional if absent
     */
    CompletionStage<Result<Optional<Transaction>>> transaction(String idempotencyKey);

    /**
     * Reads a player's balance in the default currency.
     *
     * @param playerId the player's UUID; offline players are supported
     * @return a balance result, completed asynchronously
     */
    default CompletionStage<Result<Balance>> balance(UUID playerId) {
        return balance(playerId, defaultCurrency().id());
    }

    /**
     * Reads a player's balance in a specified virtual currency.
     *
     * @param playerId the player's UUID, not a ledger account UUID
     * @param currencyId a configured virtual currency ID, such as {@code "coins"}
     * @return a balance result, completed asynchronously
     */
    CompletionStage<Result<Balance>> balance(UUID playerId, String currencyId);

    /**
     * Atomically transfers default-currency money between two players.
     *
     * @param fromPlayerId player paying the amount
     * @param toPlayerId player receiving the amount
     * @param amount positive exact decimal amount
     * @param idempotencyKey stable key unique to this business operation
     * @return the committed transaction receipt, or a failed result
     */
    default CompletionStage<Result<Transaction>> transfer(
            UUID fromPlayerId, UUID toPlayerId, BigDecimal amount, String idempotencyKey) {
        return transfer(fromPlayerId, toPlayerId, defaultCurrency().id(), amount, idempotencyKey);
    }

    /**
     * Atomically transfers money between two players in one virtual currency.
     * No money is credited unless the corresponding debit commits.
     *
     * @param fromPlayerId player paying the amount
     * @param toPlayerId player receiving the amount
     * @param currencyId configured virtual currency ID
     * @param amount positive exact decimal amount
     * @param idempotencyKey stable key unique to this business operation
     * @return the committed transaction receipt, or a failed result
     */
    CompletionStage<Result<Transaction>> transfer(
            UUID fromPlayerId, UUID toPlayerId, String currencyId, BigDecimal amount, String idempotencyKey);

    /**
     * Pays a player from the configured server payout account in the default currency.
     * This is a ledger transfer, not a mint operation; the payout account must have sufficient funds.
     *
     * @param playerId player receiving the amount
     * @param amount positive exact decimal amount
     * @param idempotencyKey stable key unique to this business operation
     * @return the committed transaction receipt, or a failed result
     */
    default CompletionStage<Result<Transaction>> deposit(UUID playerId, BigDecimal amount, String idempotencyKey) {
        return deposit(playerId, defaultCurrency().id(), amount, idempotencyKey);
    }

    /**
     * Pays a player from the configured server payout account in a specified virtual currency.
     * The same funded payout account is used by the Vault integration.
     *
     * @param playerId player receiving the amount
     * @param currencyId configured virtual currency ID
     * @param amount positive exact decimal amount
     * @param idempotencyKey stable key unique to this business operation
     * @return the committed transaction receipt, or a failed result
     */
    CompletionStage<Result<Transaction>> deposit(
            UUID playerId, String currencyId, BigDecimal amount, String idempotencyKey);

    /**
     * Charges a player into the configured server payout account in the default currency.
     * The player must have sufficient funds; this does not destroy money.
     *
     * @param playerId player paying the amount
     * @param amount positive exact decimal amount
     * @param idempotencyKey stable key unique to this business operation
     * @return the committed transaction receipt, or a failed result
     */
    default CompletionStage<Result<Transaction>> withdraw(UUID playerId, BigDecimal amount, String idempotencyKey) {
        return withdraw(playerId, defaultCurrency().id(), amount, idempotencyKey);
    }

    /**
     * Charges a player into the configured server payout account in a specified virtual currency.
     *
     * @param playerId player paying the amount
     * @param currencyId configured virtual currency ID
     * @param amount positive exact decimal amount
     * @param idempotencyKey stable key unique to this business operation
     * @return the committed transaction receipt, or a failed result
     */
    CompletionStage<Result<Transaction>> withdraw(
            UUID playerId, String currencyId, BigDecimal amount, String idempotencyKey);
}
