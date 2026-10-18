package com.cmriddles.thenewereconomy.api.request;
import com.cmriddles.thenewereconomy.api.account.AccountRef;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
/** A request asks a player for consent; creating it never debits them. */
public record RequestDraft(AccountRef requester, UUID payer, String currency, BigDecimal amount,
        String message, Optional<Instant> expiresAt, String key) { }
