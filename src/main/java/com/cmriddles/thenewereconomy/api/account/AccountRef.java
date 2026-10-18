package com.cmriddles.thenewereconomy.api.account;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies either a player or an existing ledger account without confusing their UUIDs.
 *
 * <p>Use {@link #player(UUID)} when passing a Minecraft player's UUID. Use {@link #account(UUID)}
 * when passing an account UUID returned by TNE or provided by the server owner. A reference does
 * not create an account, grant access, prove existence, or bypass ledger restrictions.
 *
 * @param id the player's UUID or ledger account's UUID, according to {@code type}
 * @param type how TNE should resolve the UUID
 */
public record AccountRef(UUID id, Type type) {
    public AccountRef {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
    }

    /**
     * Refers to a Minecraft player by their player UUID.
     *
     * @param playerId Minecraft player's UUID, including offline players
     * @return a reference TNE resolves through its player account mapping
     */
    public static AccountRef player(UUID playerId) {
        return new AccountRef(playerId, Type.PLAYER);
    }

    /**
     * Refers directly to an existing TNE ledger account.
     *
     * @param accountId ledger account UUID, not a display name or plugin account key
     * @return a reference to that ledger account
     */
    public static AccountRef account(UUID accountId) {
        return new AccountRef(accountId, Type.LEDGER);
    }

    /** The meaning of the UUID stored in a reference. */
    public enum Type {
        /** Minecraft player UUID; resolve through TNE's player account mapping. */
        PLAYER,
        /** Actual TNE ledger account UUID. */
        LEDGER
    }
}
