package com.cmriddles.thenewereconomy.api.currency;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Locale;
import java.util.Objects;

public record CurrencyId(String value) {
    private static final String PATTERN = "[a-z][a-z0-9_-]{1,63}";

    public CurrencyId {
        Objects.requireNonNull(value, "value");
        value = value.toLowerCase(Locale.ROOT);
        if (!value.matches(PATTERN)) {
            throw new ValidationException("Currency ID must match " + PATTERN);
        }
    }
}
