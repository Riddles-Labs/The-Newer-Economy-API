package com.cmriddles.thenewereconomy.api.federation;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record FederationPeerId(String value) {
    private static final Pattern PATTERN = Pattern.compile("[a-z0-9][a-z0-9-]{1,63}");

    public FederationPeerId {
        value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
        if (!PATTERN.matcher(value).matches()) {
            throw new ValidationException("Federation peer ID must match " + PATTERN.pattern());
        }
    }
}
