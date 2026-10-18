package com.cmriddles.thenewereconomy.api.account;

import com.cmriddles.thenewereconomy.api.result.Result;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Asynchronous account creation, lookup, listing, and naming for one integration.
 *
 * <p>Get this service from {@link com.cmriddles.thenewereconomy.api.EconomyClient#accounts()}.
 * Operations follow the client's readiness and result rules. They do not issue money, edit
 * currency definitions, change the configured scope, or bypass ledger restrictions.
 */
public interface AccountService {
    /**
     * Creates an initially unfunded named account, or returns this integration's existing account
     * with the same stable key.
     *
     * <p>Use the same integration name and key after restarts to recover the same persisted account
     * UUID. Repeating creation retains the original name; use {@link #rename(String, String)} to
     * change it. This account key is independent of transaction idempotency keys. Fund the account
     * by transferring existing money from an authorized source account.
     *
     * @param key case-insensitive key, 1 to 64 ASCII letters, digits, dots, underscores, colons or hyphens;
     *            must start with a letter or digit and is normalized to lowercase
     * @param displayName readable name, 1 to 64 characters with no control characters
     * @return the created or existing account, completed asynchronously
     */
    CompletionStage<Result<Account>> create(String key, String displayName);

    /**
     * Finds one account owned by this integration using its stable key, without creating it.
     *
     * @param key the integration-local account key passed to {@link #create(String, String)}
     * @return a successful optional account, empty when no such account exists
     */
    CompletionStage<Result<Optional<Account>>> find(String key);

    /**
     * Looks up a known ledger account UUID, such as a plugin account, Treasury, Rewards, or a player account.
     * This does not create an account or list arbitrary player accounts.
     *
     * @param accountId actual ledger account UUID
     * @return a successful optional account, empty when the UUID is unknown
     */
    default CompletionStage<Result<Optional<Account>>> find(UUID accountId) {
        return find(accountId == null ? null : AccountRef.account(accountId));
    }

    /**
     * Resolves an explicit player or known ledger account reference.
     * The reference's type does not change authoritative account ownership or state.
     *
     * @param reference explicit player or ledger account reference
     * @return a successful optional account, empty when it is unknown
     */
    CompletionStage<Result<Optional<Account>>> find(AccountRef reference);

    /**
     * Lists this integration's named accounts in stable order with bounded pagination.
     * This does not return all players or other integrations' accounts.
     *
     * @param limit maximum number of accounts to return, from 1 through 100
     * @param offset zero-based number of accounts to skip; must be nonnegative
     * @return an immutable page, empty when there are no more accounts
     */
    CompletionStage<Result<List<Account>>> list(int limit, int offset);

    /**
     * Changes the display name of one account owned by this integration.
     * The stable account key, ledger UUID, balances, and historical ledger entries do not change.
     *
     * @param key this integration's existing account key
     * @param displayName new readable name, 1 to 64 characters with no control characters
     * @return the renamed account, or a failed result if it does not exist
     */
    CompletionStage<Result<Account>> rename(String key, String displayName);
}
