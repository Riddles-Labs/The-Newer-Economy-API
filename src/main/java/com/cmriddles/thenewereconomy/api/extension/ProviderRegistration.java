package com.cmriddles.thenewereconomy.api.extension;
/** A registered provider's stable identifier and process-local lifetime. */
public interface ProviderRegistration extends Registration {
    /** @return the identifier accepted by external provider operations */
    String id();
}
