package com.cmriddles.thenewereconomy.api.asset;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Objects;
import java.util.UUID;

public record AssetOperationId(UUID value) {
    public AssetOperationId {
        Objects.requireNonNull(value, "value");
        if (value.equals(new UUID(0L, 0L))) {
            throw new ValidationException("Asset operation ID must not be the zero UUID");
        }
    }
}
