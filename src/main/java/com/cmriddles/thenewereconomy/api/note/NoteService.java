package com.cmriddles.thenewereconomy.api.note;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
/** Signed physical notes, with player interaction on the player's owning scheduler. */
public interface NoteService {
    CompletionStage<Result<Optional<MoneyNote>>> inspect(UUID noteId);
    /** Funds backing and attempts physical delivery. REVIEW_REQUIRED must never be blindly retried. */
    CompletionStage<Result<NoteAction>> issue(UUID player, String currency, BigDecimal amount, String idempotencyKey);
    /** Verifies the player's held signed item; a note ID alone can never redeem funds. */
    CompletionStage<Result<NoteAction>> redeemHeld(UUID player, String idempotencyKey);
}
