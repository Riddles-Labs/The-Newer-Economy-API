package com.cmriddles.thenewereconomy.api.transaction;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Objects;

public record IdempotencyKey(String value) {
    private static final int MAX_LENGTH = 128;
    private static final String PATTERN = "[A-Za-z0-9._:-]+";

    public IdempotencyKey {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || value.length() > MAX_LENGTH || !value.matches(PATTERN)) {
            throw new ValidationException("Idempotency key must be non-blank and URL-safe");
        }
    }
}
