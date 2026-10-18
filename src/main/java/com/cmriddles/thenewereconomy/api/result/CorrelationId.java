package com.cmriddles.thenewereconomy.api.result;

import java.util.Objects;
import java.util.UUID;

public record CorrelationId(UUID value) {
    public CorrelationId {
        Objects.requireNonNull(value, "value");
    }
}
