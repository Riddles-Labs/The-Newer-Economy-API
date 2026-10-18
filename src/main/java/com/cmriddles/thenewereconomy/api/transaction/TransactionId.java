package com.cmriddles.thenewereconomy.api.transaction;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(UUID value) {
    public TransactionId {
        Objects.requireNonNull(value, "value");
    }
}
