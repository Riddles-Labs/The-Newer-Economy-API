package com.cmriddles.thenewereconomy.api.economy;

import com.cmriddles.thenewereconomy.api.currency.CurrencyId;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable snapshot of one player's balance in one currency and scope.
 * A later transaction may change this balance; use a transfer/withdraw result to decide whether a payment succeeded.
 *
 * @param playerId player's UUID, not the internal ledger account UUID
 * @param currency canonical lowercase currency ID
 * @param scope configured scope of this balance
 * @param amount exact decimal balance, using configured precision when no rounding is required;
 *               finer historical fractions remain intact
 * @param version ledger version at the time of the read, useful for diagnostics
 */
public record Balance(UUID playerId, String currency, ScopeId scope, BigDecimal amount, long version) {
    public Balance {
        Objects.requireNonNull(playerId, "playerId");
        currency = new CurrencyId(currency).value();
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(amount, "amount");
        if (version < 0) throw new ValidationException("Balance version must not be negative");
    }
}
