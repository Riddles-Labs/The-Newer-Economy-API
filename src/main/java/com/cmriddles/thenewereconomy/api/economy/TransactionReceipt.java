package com.cmriddles.thenewereconomy.api.economy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Complete immutable receipt for one committed ledger transaction.
 *
 * <p>Entries contain signed changes and readable account names. Debits and credits balance for each
 * currency/scope; there is no single total amount for a multi-currency or split transaction.
 * Current account names can differ from their names at posting. Metadata is diagnostic information,
 * not an authorization signal. Keep your own durable business record for external delivery state.
 *
 * @param transactionId persistent UUID for this transaction
 * @param correlationId original stored diagnostic UUID for tracing this transaction through audit
 *     records
 * @param integrationName recorded integration attribution, or the recorded source when no
 *     integration name exists
 * @param idempotencyKey recorded operation key in its original source namespace
 * @param createdAt transaction's recorded creation time
 * @param entries complete signed ledger entries; defensively copied
 * @param metadata transaction audit/business metadata; defensively copied
 */
public record TransactionReceipt(
        UUID transactionId,
        UUID correlationId,
        String integrationName,
        String idempotencyKey,
        Instant createdAt,
        List<TransactionEntry> entries,
        Map<String, String> metadata) {
    public TransactionReceipt {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(integrationName, "integrationName");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(createdAt, "createdAt");
        entries = List.copyOf(entries);
        metadata = Map.copyOf(metadata);
    }
}
