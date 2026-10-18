package com.cmriddles.thenewereconomy.api.refund;

import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import java.math.BigDecimal;
import java.util.UUID;

/** Snapshot only: another refund can consume this allowance before a later request commits. */
public record RefundAllowance(UUID originalTransactionId, int creditSequence, int debitSequence, String currency,
        ScopeId scope, BigDecimal creditRemaining, BigDecimal debitRemaining, BigDecimal refundable) { }
