package com.cmriddles.thenewereconomy.api.federation;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Objects;
import java.util.UUID;

public record FederatedSettlementId(UUID value) {
    public FederatedSettlementId {
        Objects.requireNonNull(value, "value");
        if (value.version() != 4) {
            throw new ValidationException("Federated settlement ID must be a random UUID");
        }
    }

    public static FederatedSettlementId parse(String value) {
        Objects.requireNonNull(value, "value");
        return new FederatedSettlementId(UUID.fromString(value));
    }
}
