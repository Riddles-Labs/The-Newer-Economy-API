package com.cmriddles.thenewereconomy.api.economy;

import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.currency.CurrencyId;
import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One funded movement to include in an atomic {@link TransactionRequest}.
 *
 * <p>References identify players or existing ledger accounts. TNE resolves their authoritative
 * state when posting; constructing this object does not reserve money or grant account access.
 *
 * @param source account paying this amount
 * @param destination account receiving this amount
 * @param currency configured virtual currency ID, normalized to lowercase
 * @param amount positive exact decimal amount; the configured currency precision is checked at
 *     posting
 */
public record Payment(
        AccountRef source, AccountRef destination, String currency, BigDecimal amount) {
    public Payment {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        currency = new CurrencyId(currency).value();
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) throw new ValidationException("Payment amount must be positive");
    }
}
