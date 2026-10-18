package com.cmriddles.thenewereconomy.api.economy;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import com.cmriddles.thenewereconomy.api.transaction.IdempotencyKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable request to commit multiple funded payments as one atomic ledger transaction.
 *
 * <p>All payments commit together or none commit. This does not make external inventory, world,
 * or plugin database changes atomic with TNE. Use one durably saved idempotency key for the whole
 * request. Recover a lost response with the client's full {@code receipt(key)} method.
 * Funding is checked on the net debit for each account, currency, and scope across the whole
 * request; incoming payments in this request can fund outgoing payments in the same request.
 *
 * <p>Constructors validate nulls, basic sizes, and key syntax synchronously. Posting validates
 * currency precision, account eligibility, metadata restrictions, and sufficient funding through
 * its asynchronous result. Metadata describes your business operation; attribution, system,
 * and reversal fields are reserved for TNE and cannot be supplied to spoof their meaning.
 *
 * @param payments 1 to 100 funded movements; copied into an immutable list
 * @param idempotencyKey stable integration-local key for this entire transaction
 * @param metadata up to 16 diagnostic/business fields; keys up to 64 characters, values up to 512 characters
 */
public record TransactionRequest(List<Payment> payments, String idempotencyKey, Map<String, String> metadata) {
    /** Maximum number of payment instructions in one atomic request. */
    public static final int MAX_PAYMENTS = 100;
    /** Maximum number of caller metadata fields in one request. */
    public static final int MAX_METADATA_ENTRIES = 16;
    /** Maximum length of each caller metadata key. */
    public static final int MAX_METADATA_KEY_LENGTH = 64;
    /** Maximum length of each caller metadata value. */
    public static final int MAX_METADATA_VALUE_LENGTH = 512;

    public TransactionRequest {
        Objects.requireNonNull(payments, "payments");
        Objects.requireNonNull(metadata, "metadata");
        if (payments.isEmpty() || payments.size() > MAX_PAYMENTS) {
            throw new ValidationException("Transaction must contain 1 to " + MAX_PAYMENTS + " payments");
        }
        if (metadata.size() > MAX_METADATA_ENTRIES) {
            throw new ValidationException("Transaction metadata must contain at most " + MAX_METADATA_ENTRIES + " fields");
        }
        payments = List.copyOf(payments);
        idempotencyKey = new IdempotencyKey(idempotencyKey).value();
        metadata = Map.copyOf(metadata);
        for (var field : metadata.entrySet()) {
            if (field.getKey().isBlank() || field.getKey().length() > MAX_METADATA_KEY_LENGTH
                    || field.getValue().length() > MAX_METADATA_VALUE_LENGTH) {
                throw new ValidationException("Transaction metadata keys must contain 1 to 64 characters and values at most 512");
            }
        }
    }

    /**
     * Creates an atomic request without caller metadata.
     *
     * @param payments 1 to 100 funded movements
     * @param idempotencyKey stable key for the entire transaction
     */
    public TransactionRequest(List<Payment> payments, String idempotencyKey) {
        this(payments, idempotencyKey, Map.of());
    }
}
