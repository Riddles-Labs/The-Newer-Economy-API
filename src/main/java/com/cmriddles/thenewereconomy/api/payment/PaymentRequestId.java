package com.cmriddles.thenewereconomy.api.payment;

import java.util.Objects;
import java.util.UUID;

public record PaymentRequestId(UUID value) {
    public PaymentRequestId {
        Objects.requireNonNull(value, "value");
    }
}
