package com.cmriddles.thenewereconomy.api.refund;

/** Disputes record review decisions; changing their state never moves money. */
public enum DisputeState {
    OPEN,
    RESOLVED,
    REJECTED
}
