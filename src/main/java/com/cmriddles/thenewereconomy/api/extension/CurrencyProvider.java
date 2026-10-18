package com.cmriddles.thenewereconomy.api.extension;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
/** Adapter for an external value store. Its balances are separate from TNE's virtual ledger. */
public interface CurrencyProvider {
 CompletionStage<Result<Balance>> balance(String accountKey);
 CompletionStage<Result<Receipt>> apply(Operation operation);
 /** Must query the original durable key, never submit the operation again. */
 CompletionStage<Result<Optional<Receipt>>> recover(String operationKey);
 enum Capability { BALANCE, DEBIT, CREDIT, TRANSFER, RESERVE, CAPTURE, RELEASE, RECOVERY }
 record Balance(String accountKey, BigDecimal available, BigDecimal reserved) {
  public Balance { java.util.Objects.requireNonNull(accountKey); java.util.Objects.requireNonNull(available); java.util.Objects.requireNonNull(reserved); }
 }
 record Operation(Capability action, String accountKey, Optional<String> destinationKey,
   BigDecimal amount, String idempotencyKey, Optional<Instant> expiresAt) {
  public Operation { java.util.Objects.requireNonNull(action); java.util.Objects.requireNonNull(accountKey);
   destinationKey=java.util.Objects.requireNonNull(destinationKey); java.util.Objects.requireNonNull(amount);
   java.util.Objects.requireNonNull(idempotencyKey); expiresAt=java.util.Objects.requireNonNull(expiresAt); }
 }
 enum State { PENDING, CONFIRMED, REJECTED, REVIEW_REQUIRED }
 record Receipt(String operationKey, State state, String reference, String detail) {
  public Receipt { java.util.Objects.requireNonNull(operationKey); java.util.Objects.requireNonNull(state);
   java.util.Objects.requireNonNull(reference); java.util.Objects.requireNonNull(detail); }
 }
 record Info(String id, String integrationName, String name, String symbol, int scale, Set<Capability> capabilities) {
  public Info { capabilities=Set.copyOf(capabilities); }
 }
}
