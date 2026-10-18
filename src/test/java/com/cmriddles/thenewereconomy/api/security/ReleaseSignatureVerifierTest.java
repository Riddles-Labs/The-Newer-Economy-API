package com.cmriddles.thenewereconomy.api.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ReleaseSignatureVerifierTest {
    @Test
    void verifiesPinnedKeysAndRejectsUnsignedTamperedAndCrossProductReleases() throws Exception {
        var key = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        var verifier =
                new ReleaseSignatureVerifier(
                        Map.of(
                                "test-key",
                                Base64.getEncoder().encodeToString(key.getPublic().getEncoded())));
        for (String product : java.util.List.of("TNE-Economy", "TNECORE")) {
            var manifest =
                    new HashMap<>(
                            Map.of(
                                    "signatureVersion",
                                    "1",
                                    "product",
                                    product,
                                    "keyId",
                                    "test-key",
                                    "version",
                                    "1.0.32",
                                    "sha256",
                                    "a".repeat(64),
                                    "size",
                                    "1000"));
            if (product.equals("TNECORE")) {
                manifest.put("buildNumber", "123");
                manifest.put("revision", "b".repeat(40));
            } else {
                manifest.put("apiVersion", "26.1");
                manifest.put("javaVersion", "25");
            }
            var signer = Signature.getInstance("Ed25519");
            signer.initSign(key.getPrivate());
            signer.update(ReleaseSignatureVerifier.canonical(manifest));
            manifest.put("signature", Base64.getEncoder().encodeToString(signer.sign()));
            verifier.verify(manifest, product);
            for (String name : manifest.keySet()) {
                var tampered = new HashMap<>(manifest);
                tampered.put(name, "forged");
                assertThatThrownBy(() -> verifier.verify(tampered, product))
                        .isInstanceOf(IllegalArgumentException.class);
            }
            var unsigned = new HashMap<>(manifest);
            unsigned.remove("signature");
            assertThatThrownBy(() -> verifier.verify(unsigned, product))
                    .isInstanceOf(IllegalArgumentException.class);
            var extra = new HashMap<>(manifest);
            extra.put("downloadUrl", "https://evil.example");
            assertThatThrownBy(() -> verifier.verify(extra, product))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(
                            () ->
                                    verifier.verify(
                                            manifest,
                                            product.equals("TNECORE") ? "TNE-Economy" : "TNECORE"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ReleaseSignatureVerifier().verify(manifest, product))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
