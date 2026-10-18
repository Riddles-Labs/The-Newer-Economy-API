package com.cmriddles.thenewereconomy.api.account;

/** The authoritative owner category of a resolved ledger account. */
public enum AccountType {
    /** An account belonging to a known Minecraft player. */
    PLAYER,
    /** A known server/system account, such as Treasury, Rewards, or settlement. */
    SYSTEM,
    /** A named account created and persisted for a plugin integration. */
    PLUGIN
}
