package com.cmriddles.thenewereconomy.api.federation;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Objects;
import java.util.UUID;

public record RemoteDeliveryId(UUID value) {
    public RemoteDeliveryId {
        Objects.requireNonNull(value, "value");
        if (value.version() != 4) {
            throw new ValidationException("Remote delivery ID must be a random UUID");
        }
    }

    public static RemoteDeliveryId parse(String value) {
        Objects.requireNonNull(value, "value");
        return new RemoteDeliveryId(UUID.fromString(value));
    }
}
