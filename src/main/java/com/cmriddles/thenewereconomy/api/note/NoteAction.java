package com.cmriddles.thenewereconomy.api.note;
import java.util.Optional;
import java.util.UUID;
/** A physical-note result separates committed money movement from inventory delivery certainty. */
public record NoteAction(UUID noteId, Optional<UUID> transactionId, Delivery delivery) {
    public enum Delivery { DELIVERED, REDEEMED, NOT_DELIVERED, REVIEW_REQUIRED }
}
