package com.cmriddles.thenewereconomy.api.reservation;

import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Durable funded holds. Money and state changes commit atomically; creating a hold never mints
 * funds. Queries and commands are asynchronous and restricted to this integration's reservation
 * namespace.
 */
public interface ReservationService {
    /**
     * Creates or resumes the same saved hold terms using a stable key. Funding physically moves the
     * payment amount from its source into protected escrow. A failed funding attempt leaves PENDING
     * state for the same-key retry or release; it does not reduce the source balance. Expiry
     * instants are persisted to millisecond precision and are not renewed by retrying.
     */
    CompletionStage<Result<Reservation>> reserve(ReservationRequest request);

    CompletionStage<Result<Optional<Reservation>>> find(UUID id);

    CompletionStage<Result<Optional<Reservation>>> find(String key);

    /** Pays the fixed destination from escrow. Capture and its state transition commit together. */
    CompletionStage<Result<Reservation>> capture(UUID id);

    /** Returns escrow to its original source; cannot undo an already captured payment. */
    CompletionStage<Result<Reservation>> release(UUID id);

    /**
     * Reads one coherent available/reserved/total snapshot, including all integrations' active
     * holds on this source.
     */
    CompletionStage<Result<FundsBalance>> balances(AccountRef account, String currency);
}
