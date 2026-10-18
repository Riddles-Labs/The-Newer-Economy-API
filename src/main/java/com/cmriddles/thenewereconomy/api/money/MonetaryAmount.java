package com.cmriddles.thenewereconomy.api.money;

import com.cmriddles.thenewereconomy.api.currency.CurrencyId;
import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.math.BigDecimal;
import java.util.Objects;

public record MonetaryAmount(CurrencyId currencyId, BigDecimal amount, int scale) {
    public static final int MAX_SCALE = 18;

    public MonetaryAmount {
        Objects.requireNonNull(currencyId, "currencyId");
        Objects.requireNonNull(amount, "amount");
        requireStorageRange(amount);
        if (scale < 0 || scale > MAX_SCALE) {
            throw new ValidationException("Monetary scale must be between 0 and " + MAX_SCALE);
        }
        if (amount.scale() > scale) {
            throw new ValidationException("Monetary amount has unsupported precision");
        }
        amount = amount.setScale(scale);
    }

    public static MonetaryAmount of(CurrencyId currencyId, String decimal, int scale) {
        Objects.requireNonNull(decimal, "decimal");
        return new MonetaryAmount(currencyId, parseDecimal(decimal), scale);
    }

    /** Bounds untrusted decimal text before normalization, arithmetic, or plain-text formatting. */
    public static BigDecimal parseDecimal(String decimal) {
        Objects.requireNonNull(decimal, "decimal");
        if (decimal.length() > 80) throw new NumberFormatException("Decimal input is too long");
        BigDecimal value = new BigDecimal(decimal);
        try {
            requireStorageRange(value);
        } catch (ValidationException invalid) {
            throw new NumberFormatException(invalid.getMessage());
        }
        return value;
    }

    private static void requireStorageRange(BigDecimal value) {
        // SQL balances use DECIMAL(38,18). Use long arithmetic for adversarial exponent values.
        if (value.scale() > MAX_SCALE || (long) value.precision() - value.scale() > 20L) {
            throw new ValidationException("Monetary amount exceeds the supported decimal range");
        }
    }
}
