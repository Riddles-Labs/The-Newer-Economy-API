package com.cmriddles.thenewereconomy.api.exchange;

import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Exact server-held conversion offer. Confirm its opaque ID; do not reconstruct a trusted quote.
 */
public record ExchangeQuote(
        UUID id,
        UUID player,
        ScopeId scope,
        String sourceCurrency,
        String targetCurrency,
        BigDecimal sourceAmount,
        BigDecimal grossTargetAmount,
        BigDecimal fee,
        BigDecimal tax,
        BigDecimal netTargetAmount,
        BigDecimal rate,
        Instant expiresAt) {}
