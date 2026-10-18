package com.cmriddles.thenewereconomy.api.extension;

import java.util.Set;

/**
 * Feature discovery, independent of server readiness. Currency support remains operation-specific.
 */
public record ApiCapabilities(String version, Set<String> features) {
    public ApiCapabilities {
        features = Set.copyOf(features);
    }

    public boolean supports(String feature) {
        return features.contains(feature);
    }
}
