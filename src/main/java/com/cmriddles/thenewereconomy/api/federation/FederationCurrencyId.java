package com.cmriddles.thenewereconomy.api.federation;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record FederationCurrencyId(String value) {
    private static final Pattern PATTERN =
            Pattern.compile("[a-z][a-z0-9_-]{1,63}:[a-z][a-z0-9_-]{1,63}");
    public static final FederationCurrencyId TNE_SETTLEMENT =
            new FederationCurrencyId("tne:the-newer-economy");

    public FederationCurrencyId {
        Objects.requireNonNull(value, "value");
        value = value.toLowerCase(Locale.ROOT);
        if (!PATTERN.matcher(value).matches()) {
            throw new ValidationException("Federation currency ID must match " + PATTERN.pattern());
        }
    }

    public String namespace() {
        return value.substring(0, value.indexOf(':'));
    }

    public String localName() {
        return value.substring(value.indexOf(':') + 1);
    }
}
