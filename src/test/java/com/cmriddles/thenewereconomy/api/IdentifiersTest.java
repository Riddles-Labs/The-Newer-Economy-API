package com.cmriddles.thenewereconomy.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cmriddles.thenewereconomy.api.currency.CurrencyId;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import com.cmriddles.thenewereconomy.api.federation.FederationCurrencyId;
import com.cmriddles.thenewereconomy.api.result.ValidationException;
import com.cmriddles.thenewereconomy.api.transaction.IdempotencyKey;
import com.cmriddles.thenewereconomy.api.transaction.SourceNamespace;
import org.junit.jupiter.api.Test;

final class IdentifiersTest {
    @Test
    void invalidIdentifiersAreRejected() {
        assertThatThrownBy(() -> new CurrencyId("Dollars!"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new ScopeId(" ")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new IdempotencyKey("bad key"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new SourceNamespace("1bad"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new FederationCurrencyId("coin"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new FederationCurrencyId("1bad:coin"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new FederationCurrencyId("tne:!"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void federationCurrencyIdsAreNamespacedAndNormalized() {
        FederationCurrencyId currencyId = new FederationCurrencyId("TNE:The-Newer-Economy");

        assertThat(currencyId.value()).isEqualTo("tne:the-newer-economy");
        assertThat(currencyId.namespace()).isEqualTo("tne");
        assertThat(currencyId.localName()).isEqualTo("the-newer-economy");
        assertThat(FederationCurrencyId.TNE_SETTLEMENT.value()).isEqualTo("tne:the-newer-economy");
    }
}
