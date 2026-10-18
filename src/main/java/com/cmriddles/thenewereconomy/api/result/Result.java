package com.cmriddles.thenewereconomy.api.result;

import java.util.Objects;
import java.util.Optional;

/**
 * Outcome of an economy operation: exactly one successful value or one failure.
 *
 * <p>First check {@link #isSuccess()}. If true, {@link #value()} contains the result and {@link
 * #failure()} is null. Otherwise {@link #failure()} describes the problem and {@link #value()} is
 * null. The optional accessors offer the same information without null values. Expected validation,
 * balance, availability, and conflict failures use this result rather than exceptional completion
 * of a future.
 *
 * @param <T> successful result type
 * @param value successful value, or null on failure
 * @param failure failure details, or null on success
 */
public record Result<T>(T value, Failure failure) {
    public Result {
        if ((value == null) == (failure == null)) {
            throw new ValidationException("Result must contain exactly one value or failure");
        }
    }

    /**
     * Creates a successful result.
     *
     * @param <T> successful result type
     * @param value nonnull successful value
     * @return a successful result
     */
    public static <T> Result<T> success(T value) {
        return new Result<>(Objects.requireNonNull(value, "value"), null);
    }

    /**
     * Creates a failed result.
     *
     * @param <T> expected successful result type
     * @param failure nonnull failure details
     * @return a failed result
     */
    public static <T> Result<T> failure(Failure failure) {
        return new Result<>(null, Objects.requireNonNull(failure, "failure"));
    }

    /**
     * @return true when {@link #value()} is present; false when {@link #failure()} is present
     */
    public boolean isSuccess() {
        return failure == null;
    }

    /**
     * @return the successful value, or an empty optional on failure
     */
    public Optional<T> optionalValue() {
        return Optional.ofNullable(value);
    }

    /**
     * @return the failure details, or an empty optional on success
     */
    public Optional<Failure> optionalFailure() {
        return Optional.ofNullable(failure);
    }
}
