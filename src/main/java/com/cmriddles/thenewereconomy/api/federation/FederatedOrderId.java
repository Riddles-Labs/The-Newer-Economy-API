package com.cmriddles.thenewereconomy.api.federation;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Objects;
import java.util.UUID;

public record FederatedOrderId(UUID value) {
    public FederatedOrderId {
        Objects.requireNonNull(value, "value");
        if (value.version() != 4) {
            throw new ValidationException("Federated order ID must be a random UUID");
        }
    }

    public static FederatedOrderId parse(String value) {
        Objects.requireNonNull(value, "value");
        return new FederatedOrderId(UUID.fromString(value));
    }
}
