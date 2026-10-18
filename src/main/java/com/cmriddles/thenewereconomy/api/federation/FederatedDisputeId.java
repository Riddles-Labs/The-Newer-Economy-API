package com.cmriddles.thenewereconomy.api.federation;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Objects;
import java.util.UUID;

public record FederatedDisputeId(UUID value) {
    public FederatedDisputeId {
        Objects.requireNonNull(value, "value");
        if (value.version() != 4) {
            throw new ValidationException("Federated dispute ID must be a random UUID");
        }
    }

    public static FederatedDisputeId parse(String value) {
        Objects.requireNonNull(value, "value");
        return new FederatedDisputeId(UUID.fromString(value));
    }
}
