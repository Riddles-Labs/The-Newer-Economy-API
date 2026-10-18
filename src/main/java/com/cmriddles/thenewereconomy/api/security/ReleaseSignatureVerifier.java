package com.cmriddles.thenewereconomy.api.security;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Immutable publisher trust; download metadata cannot add a trusted signing key. */
public final class ReleaseSignatureVerifier {
    private static final Map<String, List<String>> FIELDS =
            Map.of(
                    "TNE-Economy",
                            List.of("version", "sha256", "size", "apiVersion", "javaVersion"),
                    "TNECORE", List.of("version", "sha256", "size", "buildNumber", "revision"));
    private final Map<String, String> trustedKeys;

    public ReleaseSignatureVerifier() {
        this(
                Map.of(
                        "tne-release-2026-10",
                        "MCowBQYDK2VwAyEAxL3RDQ2A/uhX+ZK/gvuJCV3TJA2ykuLepwxePc1ePrs="));
    }

    public ReleaseSignatureVerifier(Map<String, String> trustedKeys) {
        this.trustedKeys = Map.copyOf(trustedKeys);
    }

    public void verify(Map<String, String> manifest, String product) {
        try {
            if (!product.equals(manifest.get("product"))
                    || !"1".equals(manifest.get("signatureVersion")))
                throw new IllegalArgumentException("Invalid release signature protocol or product");
            String keyId = manifest.get("keyId");
            if (keyId == null) throw new IllegalArgumentException("Missing release signing key");
            String encodedKey = trustedKeys.get(keyId);
            if (encodedKey == null)
                throw new IllegalArgumentException("Untrusted release signing key");
            byte[] signed = Base64.getDecoder().decode(manifest.getOrDefault("signature", ""));
            if (signed.length != 64)
                throw new IllegalArgumentException("Missing or invalid release signature");
            var key =
                    KeyFactory.getInstance("Ed25519")
                            .generatePublic(
                                    new X509EncodedKeySpec(Base64.getDecoder().decode(encodedKey)));
            var verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(canonical(manifest));
            if (!verifier.verify(signed))
                throw new IllegalArgumentException("Release signature verification failed");
        } catch (java.security.GeneralSecurityException failure) {
            throw new IllegalArgumentException("Release signature verification failed", failure);
        }
    }

    public static byte[] canonical(Map<String, String> manifest) {
        var productFields = FIELDS.get(manifest.get("product"));
        if (productFields == null || !"1".equals(manifest.get("signatureVersion")))
            throw new IllegalArgumentException("Invalid release signature protocol");
        var names = new java.util.ArrayList<>(List.of("signatureVersion", "product", "keyId"));
        names.addAll(productFields);
        var allowed = new java.util.HashSet<>(names);
        allowed.add("signature");
        if (!allowed.containsAll(manifest.keySet()))
            throw new IllegalArgumentException("Unexpected unsigned manifest field");
        var result = new StringBuilder("TNE-RELEASE-SIGNATURE-v1\n");
        for (String name : names) {
            String value = manifest.get(name);
            if (value == null || value.length() > 128 || !value.matches("[A-Za-z0-9.:-]+"))
                throw new IllegalArgumentException("Invalid signed release field");
            result.append(name).append('=').append(value).append('\n');
        }
        return result.toString().getBytes(StandardCharsets.UTF_8);
    }
}
