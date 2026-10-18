package com.cmriddles.thenewereconomy.api.asset;

import java.util.Objects;
import java.util.UUID;

public record MoneyNoteId(UUID value) {
    public MoneyNoteId {
        Objects.requireNonNull(value, "value");
    }
}
