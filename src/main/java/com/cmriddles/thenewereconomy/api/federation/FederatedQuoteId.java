package com.cmriddles.thenewereconomy.api.federation;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Objects;
import java.util.UUID;

public record FederatedQuoteId(UUID value) {
    public FederatedQuoteId {
        Objects.requireNonNull(value, "value");
        if (value.version() != 4) {
            throw new ValidationException("Federated quote ID must be a random UUID");
        }
    }

    public static FederatedQuoteId parse(String value) {
        Objects.requireNonNull(value, "value");
        return new FederatedQuoteId(UUID.fromString(value));
    }
}
