package com.cmriddles.thenewereconomy.api.result;

public final class ValidationException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
