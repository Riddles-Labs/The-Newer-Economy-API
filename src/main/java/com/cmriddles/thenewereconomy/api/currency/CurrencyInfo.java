package com.cmriddles.thenewereconomy.api.currency;

import com.cmriddles.thenewereconomy.api.money.MonetaryAmount;
import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.util.Objects;

/**
 * Immutable display and precision metadata for a virtual currency.
 *
 * @param id canonical lowercase ID to pass to economy methods; do not pass a display name
 * @param singularName configured display name for one unit
 * @param pluralName configured display name for multiple units
 * @param symbol configured display symbol
 * @param scale number of fractional digits supported, from 0 through 18
 */
public record CurrencyInfo(
        String id, String singularName, String pluralName, String symbol, int scale) {
    public CurrencyInfo {
        id = new CurrencyId(id).value();
        singularName = requireText(singularName, "singularName");
        pluralName = requireText(pluralName, "pluralName");
        symbol = requireText(symbol, "symbol");
        if (scale < 0 || scale > MonetaryAmount.MAX_SCALE) {
            throw new ValidationException(
                    "Currency scale must be between 0 and " + MonetaryAmount.MAX_SCALE);
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) throw new ValidationException(fieldName + " must not be blank");
        return value;
    }
}
