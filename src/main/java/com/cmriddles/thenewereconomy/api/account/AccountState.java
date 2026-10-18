package com.cmriddles.thenewereconomy.api.account;

/** Current account state; ledger rules continue to apply to every money operation. */
public enum AccountState {
    /** Account is active; sufficient funds and currency rules are still required. */
    ACTIVE,
    /** Account policy restricts operations. */
    RESTRICTED,
    /** Account is locked against money operations. */
    LOCKED,
    /** Account is closed. Historical ledger entries remain intact. */
    CLOSED
}
