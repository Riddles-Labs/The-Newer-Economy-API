# Account controls, delegated budgets, refunds, and disputes

These services are available from a native `EconomyClient` in TNE **1.0.64 or
later**. Obtain `TneApi` through Bukkit's services manager as described in the
[main API guide](API.md), then create your client with `api.client(getName())`.
Use Java 25 and the provided/compile-only TNE API dependency.

All service methods return `CompletionStage<Result<T>>`. Check exceptional
completion and `result.isSuccess()` before using a value. Do not block a Paper
or Folia game thread with `join()` or `get()`. Completion callbacks can run on
a worker thread; schedule player messages and inventory changes on the owning
entity thread.

## Control an account your plugin owns

`accounts()` creates and names accounts. `controls()` manages their state and
persisted metadata. Both identify your account using its stable, case-insensitive
integration-local key. Display names and metadata do not grant financial authority.

```java
economy.controls().metadata(
        "quest-rewards",
        Map.of("purpose", "Daily quest payouts", "season", "spring"),
        "Record the account's purpose");

economy.controls().state("quest-rewards", AccountState.LOCKED, "Review unusual payouts");
economy.controls().state("quest-rewards", AccountState.ACTIVE, "Review complete");
economy.controls().profile("quest-rewards");
```

The calls above show the available operations; sequence dependent calls with
`thenCompose` and inspect each result. Imports are `java.util.Map` and
`com.cmriddles.thenewereconomy.api.account.AccountState`.

Metadata replaces the account's previous metadata map. It permits at most 16
fields, keys up to 64 characters, and values up to 512 characters. Keys must be
nonblank. Control characters and reserved `tne.*` keys are rejected. Include a
readable reason of 1 to 512 characters when changing metadata or account state.
Changes are recorded transactionally in TNE's audit and event records.

`LOCKED` and `RESTRICTED` block money operations. `ACTIVE` restores an account
that has not been closed. `CLOSED` is final: it does not delete history or reopen
when `accounts().create(...)` is called again. Closing requires zero balances
in **every currency and scope**, no active grants, no pending/active reservations,
no active or paused payment schedules, and no unresolved transaction disputes.
An active reservation's escrow account cannot be locked, restricted, or closed.
Cancel or reconcile outstanding work before closing its accounts.

## Give another integration a bounded spending allowance

A grant allows one named integration to pay from one account your integration
owns, in one currency and this server's configured scope. The source account
must contain actual funds. Creating an account or grant does not issue money.

This complete asynchronous helper creates or recovers the account and then
creates or recovers its grant. It never blocks a game thread:

```java
import com.cmriddles.thenewereconomy.api.EconomyClient;
import com.cmriddles.thenewereconomy.api.access.SpendingGrant;
import com.cmriddles.thenewereconomy.api.access.SpendingGrantRequest;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

static CompletionStage<Result<SpendingGrant>> allowQuestPayouts(EconomyClient owner) {
    return owner.accounts().create("quest-rewards", "Quest rewards")
            .thenCompose(created -> {
                if (!created.isSuccess()) {
                    return CompletableFuture.completedFuture(Result.failure(created.failure()));
                }
                var request = new SpendingGrantRequest(
                        "daily-payouts",             // Stable grant key; separate from transaction keys.
                        "quest-rewards",             // The source account your integration owns.
                        "QuestPayouts",              // Exact stable name used by the delegate's client.
                        owner.defaultCurrency().id(),
                        new BigDecimal("5.00"),     // Maximum for one payment.
                        new BigDecimal("100.00"),   // Total allowance per period.
                        86_400,                     // One-day periods, anchored at grant creation.
                        Instant.now().plusSeconds(7 * 86_400));
                return owner.access().grant(request);
            });
}
```

The helper's caller should use `whenComplete((result, error) -> ...)`, handle
both failure paths, and persist `result.value().id()` on success. Fund the
returned account separately through an authorized transfer from an existing
Treasury or another funded account. Persist that transfer's operation key before
submitting it, and reconcile uncertain results with `receipt(key)`.

Inside the **QuestPayouts** plugin, using its own client:

```java
EconomyClient payouts = api.client(getName()); // getName() must be "QuestPayouts".
payouts.access().pay(
        savedGrantId,
        AccountRef.player(playerUuid),
        new BigDecimal("2.50"),
        savedQuestCompletionKey)
        .whenComplete((result, error) -> {
            if (error != null) {
                // Outcome can be uncertain. Reconcile using the same saved business record/key.
                return;
            }
            if (!result.isSuccess()) {
                // Do not deliver an external reward on failure.
                return;
            }
            // Persist result.value().transactionId() and finish external work exactly once.
        });
```

`AccountRef` is in `com.cmriddles.thenewereconomy.api.account`. The destination
can also be an existing ledger account reference. The grant fixes the source,
currency, scope, delegate, payment cap, and period budget. At commit, TNE checks
these saved terms, expiry, revocation, account eligibility, and available funds.
Budget consumption commits with the ledger. Failed or rolled-back money
movements do not spend the allowance, and concurrent requests cannot overrun it.

Periods are fixed windows anchored to the saved grant's creation time. Unspent
allowance does not carry over. Period lengths are 1 second to 365 days; expiry
must be in the next 366 days. Amounts must use the currency's exact precision;
the positive payment cap cannot exceed the positive period budget.

Grant keys are case-insensitive ASCII identifiers of 1 to 64 characters,
starting with a letter or digit. Later characters may also include `.`, `_`,
`:`, and `-`. Repeating `grant(request)` returns the existing policy: it does
not reset the budget, extend expiry, clear revocation, or replace its terms.
To change policy, revoke the old grant and create a different stable key.

Owners can use `access().revoke(id, reason)` and
`access().list(accountKey, limit, offset)`. Owners and the named delegate can use
`access().find(id)`. Pages accept limits from 1 to 100 and nonnegative offsets.

**Trust boundary:** grants are an opt-in validated payment path between trusted
server integrations. Integration names are declared attribution, not credentials.
The normal trusted account transfer API remains available. This is not a security
sandbox against installed plugins with access to the same JVM. Your plugin must
authorize its own player commands and decide which accounts users may select.

## Refund an original allocation

A full reversal uses `economy.reverse(...)`. For a partial return, use
`economy.refunds()`. A refund names two sequence numbers from the original full
receipt: the **positive credit** being returned and the **negative debit** that
originally funded it. This also works for split transactions with several payers
or recipients, without guessing which UUID represents the intended allocation.

```java
import com.cmriddles.thenewereconomy.api.refund.RefundRequest;

var request = new RefundRequest(
        savedOriginalTransactionId,
        1,                          // Original positive entry's sequence.
        0,                          // Original negative entry's sequence.
        new BigDecimal("3.00"),
        "Customer returned one item",
        savedRefundOperationKey);

economy.refunds().refund(request).whenComplete((result, error) -> {
    if (error != null || !result.isSuccess()) {
        // Keep the saved refund key. Reconcile uncertain outcomes with economy.receipt(key).
        return;
    }
    // Persist the new refund transaction UUID; the original transaction remains immutable.
});
```

Sequence numbers are the original receipt's entry order, starting at zero; do
not guess them for a split transaction. Read the stored full receipt and match
the account, sign, currency, and scope. A simple payment normally has debit 0
and credit 1. Both selected entries must be from your integration's original
transaction, in the same currency and scope, and belong to different accounts.

`refunds().remaining(originalId, creditSequence, debitSequence)` returns
`RefundAllowance`: the remaining credit, remaining debit, and minimum refundable
amount. This is a snapshot, not a reservation. At commit, TNE locks the original
transaction and checks all previous allocations again. Concurrent refunds cannot
exceed either selected original allocation, even when several recipients refund
the same original payer. The original recipient must still have sufficient funds.

A full reversal and partial allocation refunds are mutually exclusive for an
original transaction. After a partial refund, refund only remaining allocations;
do not reverse the whole original amount. After a full reversal, no partial
refund remains. Refunds and reversal transactions cannot themselves be partially
refunded. An unrelated integration cannot refund your transaction. Refunds do
not automatically undo delivered items or external database changes.

Physical note issuance/redemption and reservation lifecycle transactions cannot
use ordinary refunds or full reversals. Their ledger entries back a separate
bearer asset or escrow obligation; moving the money alone would leave that value
active. Use the appropriate lifecycle service or reconcile an uncertain note
delivery. In particular, never reverse a note issuance to recover an undelivered
item and then retry delivery of that same note.

## Record and resolve a dispute

```java
economy.refunds().openDispute(savedTransactionId, "support-ticket:381", "Delivery missing");
economy.refunds().disputes(savedTransactionId, 20, 0);
economy.refunds().resolveDispute(savedDisputeId, DisputeState.RESOLVED, "Delivery confirmed");
```

Import `com.cmriddles.thenewereconomy.api.refund.DisputeState`. These calls are
independent examples; chain dependent operations and check results. Dispute keys
use the same 1 to 64 character rules as grant keys. Repeating a key recovers its
existing dispute for that original transaction. States are `OPEN`, `RESOLVED`,
and `REJECTED`; a final decision cannot be reopened or changed into the other
final state through this API.

A dispute stores its initial reason, review explanation, and timestamps. It
never moves funds or silently issues a refund. If review calls for a refund,
submit an explicit, separately keyed refund and reconcile its receipt before
recording external delivery state. Disputes and refunds remain visible after
restarts and are recorded for auditing.
