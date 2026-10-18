package com.cmriddles.thenewereconomy.api.account;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable description of a known TNE ledger account.
 *
 * <p>Its UUID identifies the account; the display name is only a label. Plugin-owned account keys
 * are scoped to the stable integration name used to create the client. Creation, lookup, and
 * listing do not fund an account. Use {@link #ref()} with balance and transfer methods.
 *
 * @param id actual ledger account UUID; stable across restarts
 * @param name readable account name or an honest fallback when a name is unavailable
 * @param type authoritative owner category
 * @param state current account state at the time of lookup
 * @param integrationName integration owning a plugin account, empty for other account types
 * @param key stable integration-local key for a plugin account, empty for other account types
 */
public record Account(UUID id, String name, AccountType type, AccountState state,
        Optional<String> integrationName, Optional<String> key) {
    public Account {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(integrationName, "integrationName");
        Objects.requireNonNull(key, "key");
        if (name.isBlank()) throw new ValidationException("Account name must not be blank");
    }

    /** @return a direct ledger reference for balance reads and transfers using this account */
    public AccountRef ref() {
        return AccountRef.account(id);
    }
}
