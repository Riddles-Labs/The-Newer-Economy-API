package com.cmriddles.thenewereconomy.api;

import com.cmriddles.thenewereconomy.api.currency.CurrencyInfo;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import java.util.List;

/**
 * Entry point for another plugin to use TNE without depending on its implementation modules.
 *
 * <p>On Paper/Folia, obtain this interface from Bukkit's services manager. Use {@link #client(String)}
 * once with your plugin's stable name, then use that client for balance reads and payments. Do not
 * construct TNE implementation classes or shade this API into your plugin.
 *
 * <p>Metadata methods read configuration already held in memory; they perform no database work.
 * Money operations belong to {@link EconomyClient} and return asynchronous results. No Bukkit or
 * platform types appear in this interface.
 *
 * <p>The service can be registered while TNE is starting or recovering its storage connection.
 * {@link #isAvailable()} is a readiness hint, not a guarantee that a later operation will succeed.
 * Always inspect that operation's result too.
 */
public interface TneApi {
    /**
     * Returns whether the economy runtime is currently ready to accept operations.
     *
     * @return current readiness; {@code false} during startup, recovery, or shutdown
     */
    boolean isAvailable();

    /** @return the provider's supported native integration features */
    default com.cmriddles.thenewereconomy.api.extension.ApiCapabilities capabilities() {
        return new com.cmriddles.thenewereconomy.api.extension.ApiCapabilities("1.0.63", java.util.Set.of("accounts", "payments", "receipts", "history"));
    }

    /**
     * Returns the configured default virtual currency, including its display names and precision.
     * This metadata remains available while storage is unavailable.
     *
     * @return the immutable configured default currency
     */
    CurrencyInfo defaultCurrency();

    /**
     * Returns the configured scope in which all operations on this server execute.
     *
     * @return the default scope, such as {@code network} or a configured server scope
     */
    ScopeId defaultScope();

    /**
     * Lists virtual currencies available to this server's configured scope.
     * Item-backed and experience-backed currencies are not handled by this API.
     *
     * @return an immutable snapshot; the list is not filtered using any player's permissions
     */
    List<CurrencyInfo> currencies();

    /**
     * Creates a lightweight integration client for your plugin's stable name.
     *
     * <p>Use the same name after restarts, for example {@code api.client(getName())}. The name is
     * recorded for transaction auditing and isolates this integration's idempotency keys. It is
     * declared attribution, not authentication or a player permission check.
     *
     * @param integrationName stable, nonblank integration name with 1 to 128 characters and no control characters
     * @return a client using this server's default currency and scope
     * @throws IllegalArgumentException if the name is blank, too long, or contains control characters
     * @throws NullPointerException if the name is null
     */
    EconomyClient client(String integrationName);
}
