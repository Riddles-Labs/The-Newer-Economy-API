package com.cmriddles.thenewereconomy.api.economy;

import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.currency.CurrencyId;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One signed ledger entry in a committed transaction receipt.
 *
 * @param account resolved direct ledger account reference, not an unresolved player reference
 * @param accountName readable account name or an honest fallback when unavailable
 * @param currency canonical lowercase currency ID
 * @param scope configured scope where this entry committed
 * @param amount signed change: negative for a debit, positive for a credit; not a resulting balance
 */
public record TransactionEntry(AccountRef account, String accountName, String currency, ScopeId scope, BigDecimal amount) {
    public TransactionEntry {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(accountName, "accountName");
        currency = new CurrencyId(currency).value();
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(amount, "amount");
    }
}
