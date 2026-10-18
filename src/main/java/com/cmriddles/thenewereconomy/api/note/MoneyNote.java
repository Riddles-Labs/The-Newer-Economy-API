package com.cmriddles.thenewereconomy.api.note;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
/** Stored note state only; possession of this DTO does not prove possession of a signed physical item. */
public record MoneyNote(UUID id, UUID issuerAccountId, String currency, BigDecimal amount, String status,
        Instant issuedAt, Optional<Instant> expiresAt) { }
