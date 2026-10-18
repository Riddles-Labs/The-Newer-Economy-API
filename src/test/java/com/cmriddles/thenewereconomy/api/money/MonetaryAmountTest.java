package com.cmriddles.thenewereconomy.api.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cmriddles.thenewereconomy.api.currency.CurrencyId;
import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

final class MonetaryAmountTest {
    @Test
    void rejectsUnsupportedPrecision() {
        CurrencyId currencyId = new CurrencyId("dollars");

        assertThatThrownBy(() -> MonetaryAmount.of(currencyId, "1.001", 2))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("unsupported precision");
    }

    @Test
    void storesAmountAsBigDecimal() throws NoSuchMethodException {
        assertThat(MonetaryAmount.class.getDeclaredMethod("amount").getReturnType())
                .isEqualTo(BigDecimal.class);
    }
    @Test
    void rejectsExtremeExponentsBeforeExpandingOrFormattingAmounts() {
        for (String input : java.util.List.of("1E+2147483647", "1E-2147483647", "1E+10000000", "9".repeat(81))) {
            assertThatThrownBy(() -> MonetaryAmount.parseDecimal(input)).isInstanceOf(NumberFormatException.class);
        }
        assertThatThrownBy(() -> new MonetaryAmount(new CurrencyId("coin"), new BigDecimal("1E+2147483647"), 2))
                .isInstanceOf(ValidationException.class);
        assertThat(MonetaryAmount.parseDecimal("99999999999999999999.999999999999999999"))
                .isEqualByComparingTo("99999999999999999999.999999999999999999");
        assertThat(MonetaryAmount.of(new CurrencyId("coin"), "1e2", 2).amount()).isEqualByComparingTo("100.00");
    }
}
