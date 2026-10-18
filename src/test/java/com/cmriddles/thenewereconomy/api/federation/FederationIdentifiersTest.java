package com.cmriddles.thenewereconomy.api.federation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cmriddles.thenewereconomy.api.result.ValidationException;
import org.junit.jupiter.api.Test;

final class FederationIdentifiersTest {
    @Test
    void normalizesIssuerAndCurrencyIds() {
        FederationIssuerId issuerId = new FederationIssuerId("Civ-Network");
        FederationCurrencyId currencyId = new FederationCurrencyId("Civ-Network:Civ-Credits");
        FederationPeerId peerId = new FederationPeerId("Civ-Network");

        assertThat(issuerId.value()).isEqualTo("civ-network");
        assertThat(currencyId.value()).isEqualTo("civ-network:civ-credits");
        assertThat(currencyId.namespace()).isEqualTo("civ-network");
        assertThat(currencyId.localName()).isEqualTo("civ-credits");
        assertThat(peerId.value()).isEqualTo("civ-network");
        assertThat(FederationCurrencyId.TNE_SETTLEMENT.value()).isEqualTo("tne:the-newer-economy");
    }

    @Test
    void rejectsDisplayNamesAsCurrencyIdentity() {
        assertThatThrownBy(() -> new FederationCurrencyId("Civ Credits"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Federation currency ID must match");
        assertThatThrownBy(() -> new FederationCurrencyId("civ"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Federation currency ID must match");
    }

    @Test
    void normalizesMarketIds() {
        FederatedMarketId marketId = new FederatedMarketId("Market:Civ-Network:Civ-Credits:The-Newer-Economy");

        assertThat(marketId.value()).isEqualTo("market:civ-network:civ-credits:the-newer-economy");
    }

    @Test
    void rejectsNonMarketFederationIdsAsMarketIds() {
        assertThatThrownBy(() -> new FederatedMarketId("civ-network:civ-credits"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Federated market ID must match");
    }

    @Test
    void rejectsInvalidPeerIds() {
        assertThatThrownBy(() -> new FederationPeerId("civ network"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Federation peer ID must match");
        assertThatThrownBy(() -> new FederationPeerId("-civ-network"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Federation peer ID must match");
    }
}
