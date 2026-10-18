package com.cmriddles.thenewereconomy.api.economy;

import com.cmriddles.thenewereconomy.api.account.Account;
import com.cmriddles.thenewereconomy.api.currency.CurrencyId;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable balance snapshot for a resolved player, plugin, or system account.
 *
 * <p>This is not a reservation of funds. Rely on the money operation's result to determine whether
 * a payment committed. Each account has a separate balance for each currency and scope.
 *
 * @param account resolved ledger account, including its readable name and actual account UUID
 * @param currency canonical lowercase currency ID
 * @param scope configured scope of this balance
 * @param amount exact decimal balance preserving the currency's configured precision
 * @param version ledger version at the time of the read, useful for diagnostics
 */
public record AccountBalance(
        Account account, String currency, ScopeId scope, BigDecimal amount, long version) {
    public AccountBalance {
        Objects.requireNonNull(account, "account");
        currency = new CurrencyId(currency).value();
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(amount, "amount");
        if (version < 0) throw new ValidationException("Balance version must not be negative");
    }
}
