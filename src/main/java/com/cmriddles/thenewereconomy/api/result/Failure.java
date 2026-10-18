package com.cmriddles.thenewereconomy.api.result;

import java.util.Objects;

/**
 * Structured failure details. Branch on {@link #code()}, rather than parsing the message text.
 *
 * @param code machine-readable failure category
 * @param message human-readable explanation for diagnostics; not a stable protocol or a formatting template
 * @param correlationId diagnostic reference to include when reporting an operation failure
 */
public record Failure(ResultCode code, String message, CorrelationId correlationId) {
    public Failure {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(correlationId, "correlationId");
        if (message.isBlank()) {
            throw new ValidationException("Failure message must not be blank");
        }
    }
}
