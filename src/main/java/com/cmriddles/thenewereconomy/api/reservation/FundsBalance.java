package com.cmriddles.thenewereconomy.api.reservation;

import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import java.math.BigDecimal;

/**
 * Available excludes escrow; reserved includes all active holds for this source, currency and
 * scope.
 */
public record FundsBalance(
        AccountRef account,
        String currency,
        ScopeId scope,
        BigDecimal available,
        BigDecimal reserved,
        BigDecimal total) {}
