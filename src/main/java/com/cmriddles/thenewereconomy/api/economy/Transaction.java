package com.cmriddles.thenewereconomy.api.economy;

import com.cmriddles.thenewereconomy.api.currency.CurrencyId;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Receipt returned only after a money operation commits to the ledger.
 *
 * @param transactionId persistent transaction UUID for receipt lookup and administrator auditing
 * @param currency canonical lowercase currency ID
 * @param scope configured scope where the transfer committed
 * @param amount positive exact amount moved by this operation; this is not a resulting balance
 */
public record Transaction(UUID transactionId, String currency, ScopeId scope, BigDecimal amount) {
    public Transaction {
        Objects.requireNonNull(transactionId, "transactionId");
        currency = new CurrencyId(currency).value();
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) throw new ValidationException("Transaction amount must be positive");
    }
}
