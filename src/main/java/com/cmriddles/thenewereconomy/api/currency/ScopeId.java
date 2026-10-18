package com.cmriddles.thenewereconomy.api.currency;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Locale;
import java.util.Objects;

public record ScopeId(String value) {
    private static final String PATTERN = "[a-z][a-z0-9_.:-]{1,95}";

    public ScopeId {
        Objects.requireNonNull(value, "value");
        value = value.toLowerCase(Locale.ROOT);
        if (!value.matches(PATTERN)) {
            throw new ValidationException("Scope ID must match " + PATTERN);
        }
    }

    public static ScopeId network() {
        return new ScopeId("network");
    }
}
