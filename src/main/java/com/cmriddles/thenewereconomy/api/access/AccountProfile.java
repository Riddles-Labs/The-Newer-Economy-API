package com.cmriddles.thenewereconomy.api.access;

import com.cmriddles.thenewereconomy.api.account.Account;
import java.util.Map;
import java.util.Objects;

/** An owned account and its persisted integration-defined metadata. */
public record AccountProfile(Account account, Map<String, String> metadata) {
    public AccountProfile {
        Objects.requireNonNull(account, "account");
        metadata = Map.copyOf(metadata);
    }
}
