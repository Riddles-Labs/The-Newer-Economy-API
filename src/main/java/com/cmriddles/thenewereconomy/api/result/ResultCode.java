package com.cmriddles.thenewereconomy.api.result;

/** Machine-readable outcome categories shared by TNE operations. */
public enum ResultCode {
    /**
     * Operation succeeded; successful {@link Result} instances carry a value instead of a failure.
     */
    SUCCESS,
    /** An argument, amount, identifier, or other requested value is invalid. */
    VALIDATION_ERROR,
    /** An applicable operation policy denies access. */
    PERMISSION_DENIED,
    /** The paying account cannot cover the requested amount. */
    INSUFFICIENT_FUNDS,
    /** Account state or ledger policy prevents this operation. */
    ACCOUNT_RESTRICTED,
    /** Currency does not exist or is not usable for this operation in the configured scope. */
    CURRENCY_UNAVAILABLE,
    /**
     * An idempotency key has already committed, or another conflicting condition prevents the
     * operation.
     */
    CONFLICT,
    /** The runtime, storage, or another required dependency is temporarily unavailable. */
    DEPENDENCY_UNAVAILABLE,
    /**
     * An unexpected internal failure prevented a successful result; include the correlation ID in
     * diagnostics.
     */
    INTERNAL_ERROR
}
