package com.cmriddles.thenewereconomy.api.federation;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record FederatedMarketId(String value) {
    private static final Pattern PATTERN = Pattern.compile("market:[a-z][a-z0-9_-]{1,63}:[a-z][a-z0-9_-]{1,63}:[a-z][a-z0-9_-]{1,63}");

    public FederatedMarketId {
        Objects.requireNonNull(value, "value");
        value = value.toLowerCase(Locale.ROOT);
        if (!PATTERN.matcher(value).matches()) {
            throw new ValidationException("Federated market ID must match " + PATTERN.pattern());
        }
    }
}
