# Fund reservations, workflows, and scheduled payments

These services are available from the same `EconomyClient` used for ordinary
payments. They use the configured server scope and virtual currencies. They keep
state in TNE storage across restarts; your plugin does not need database access.

```java
var reservations = economy.reservations();
var workflows = economy.workflows();
var schedules = economy.schedules();
```

The examples assume an existing `EconomyClient economy` and known player/account
UUIDs. See [the API quick start](API.md) for dependency installation and service
lookup. Calls return `CompletionStage<Result<T>>`. Check exceptional completion,
then `result.isSuccess()`, before using `result.value()`. Never block a Paper or
Folia game thread. Completion handlers can run on a worker; schedule inventory,
entity, world, and player-message changes on their owning scheduler.

## Reserve money before committing a payment

A reservation fixes the paying account, receiving account, currency, amount,
and expiry. Funding moves real money from the source into protected escrow.
The source cannot spend those reserved funds through ordinary transfers, Vault,
admin adjustments, or another integration. Creating a reservation never issues
currency or creates spending power.

```java
import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.economy.Payment;
import com.cmriddles.thenewereconomy.api.reservation.ReservationRequest;
import java.math.BigDecimal;
import java.time.Instant;

// Persist this complete request with your business record BEFORE submitting it.
var request = new ReservationRequest(
        "order:123:hold",
        new Payment(AccountRef.player(buyerUuid), AccountRef.account(receivingAccountUuid),
                "coins", new BigDecimal("12.50")),
        savedExpiryInstant); // choose and persist one actual future deadline

reservations.reserve(request).whenComplete((result, error) -> {
    if (error != null || !result.isSuccess()) {
        // Retain the saved request; inspect/reconcile before retrying.
        return;
    }
    var hold = result.value();
    // Persist hold.id() and inspect hold.status(). ACTIVE means funds are held.
});
```

Use your own stable key, not a new random key on each retry. Reusing a key with
changed terms fails. Expiry is stored to millisecond precision and a retry does
not extend it. A failed initial funding attempt can leave a `PENDING` record;
no source funds moved. Retry the same saved terms after funding the source, or
release the unfunded reservation. An expired, released, or captured reservation
does not become active again merely because its original request is repeated.

After an `ACTIVE` hold is confirmed, select the appropriate final action:

```java
reservations.capture(holdId); // escrow -> the originally fixed destination
reservations.release(holdId); // escrow -> the original source
reservations.find(holdId);    // Optional<Reservation>, this integration only
reservations.find("order:123:hold");
```

Inspect these futures just like the reserve result. Capture and release are
mutually exclusive. Their ledger movement and saved state change commit in one
transaction. Retrying the same final action reads its persisted final result;
trying the opposite action after completion fails. Release cannot undo a
captured payment. Keep the saved reservation/transaction IDs for reconciliation.
`Reservation.transactionId()` identifies the most recent committed funding or
finalization movement; it is empty for an unfunded pending hold. Read its complete
receipt through `economy.receipt(transactionId)`, or inspect the escrow account's
history for both the original funding and the final capture/return.

The automatic worker returns expired ordinary holds to their source in bounded
batches. Expiry is not a promise of an exact wall-clock refund instant. Source
account restrictions, unavailable storage, or a denying policy can delay the
return; the funds remain in escrow and are not discarded.

### Available, reserved, and total

```java
reservations.balances(AccountRef.player(playerUuid), "coins")
        .whenComplete((result, error) -> {
            if (error != null || !result.isSuccess()) return;
            var funds = result.value();
            var available = funds.available();
            var reserved = funds.reserved();
            var total = funds.total(); // available + reserved
        });
```

This reads one coherent snapshot for that source account, currency, and scope.
`available` is spendable money still on the account. `reserved` is money in all
active holds funded by that account, including holds belonging to other
integrations. `total` combines those two figures. Captured payments no longer
belong in this source's total. Ordinary `balance(...)` reports the actual ledger
account balance, so it excludes money already moved into escrow.

## Coordinate multiple external steps

A workflow combines one funded reservation with an ordered list of 1 to 32
step labels. Labels describe your business process; TNE does not run or serialize
Java callbacks, inventory actions, HTTP requests, or database code for those steps.

```java
var heldPayment = new ReservationRequest(
        "order:123:workflow-funds",
        new Payment(AccountRef.player(buyerUuid), AccountRef.account(receivingAccountUuid),
                "coins", new BigDecimal("12.50")),
        savedExpiryInstant);

workflows.create("order:123:checkout", heldPayment,
        java.util.List.of("fulfilment", "external-receipt"))
        .whenComplete((result, error) -> {
            if (error != null || !result.isSuccess()) return;
            // Persist result.value().id() with the same durable business record.
        });
```

Use the same integration name, workflow key, reservation request, and step list
when recovering after a restart. Find by the saved UUID or workflow key instead
of creating an unrelated replacement. Creation can leave the initial workflow
and unfunded reservation pending if funding fails; no external step may begin
until its reservation is active.

For each step, claim it **before** starting external delivery:

```java
workflows.beginStep(workflowId).whenComplete((result, error) -> {
    if (error != null || !result.isSuccess()) return;
    var pending = result.value();
    int stepIndex = pending.currentStep();
    String label = pending.steps().get(stepIndex);
    java.util.UUID token = pending.deliveryToken().orElseThrow();
    // Save the token/step in your durable delivery record.
    // Perform your own idempotent external action on its correct thread.
});
```

Only after that external action has a durable completion receipt, acknowledge
its saved token:

```java
workflows.acknowledgeStep(workflowId, savedDeliveryToken);
```

An acknowledgement advances exactly the matching pending step. Calling
`beginStep` twice does not grant a second delivery attempt. A stale token cannot
advance another step. When every step is acknowledged, the workflow becomes
`READY_TO_CAPTURE`; call `workflows.capture(workflowId)` to pay the fixed
recipient and atomically mark the workflow `COMPLETED`.
The workflow's `reservationId()` links to its hold and final transaction ID for
receipt lookup after a lost capture response.

Before any delivery has been acknowledged, a safe cancellation can return the
funds with `workflows.cancel(workflowId)`. Cancellation is refused while delivery
is pending/unknown or after an earlier step has already been acknowledged.
A partially fulfilled process needs explicit business reconciliation, not an
automatic refund that could make completed external delivery free.

### Lost delivery response or server restart

`DELIVERY_PENDING` means the external operation **may already have happened**.
Neither a timeout nor a restart proves failure. TNE never automatically repeats
that step. Its token and progress survive a restart. After a delivery attempt
begins, pending/unknown or acknowledged delivery protects the funds from automatic
expiry; they remain held until the process is safely resolved.

```java
workflows.quarantine(workflowId, "External response was lost; receipt check required");
workflows.find(workflowId); // inspect persisted progress and delivery token
```

Reconcile against your own durable external receipt or idempotency record. Then
make an explicit, truthful assertion:

```java
workflows.recover(workflowId, true, "External receipt confirms this step completed");
// Advances this step; inspect whether another step remains or capture is ready.

workflows.recover(workflowId, false, "External system confirms no delivery occurred");
// Makes the same step ready for an explicit new delivery attempt.
```

These alternatives are mutually exclusive decisions. Do not invoke both. TNE
cannot verify the truth of an inventory or external-system assertion. A `false`
assertion is not permission to duplicate delivery; first establish that delivery
did not occur. If the outcome cannot be established, keep it quarantined and
retain the funds for an operator's review.

The ledger and external system are not one distributed transaction. Exactly-once
external delivery remains your plugin's responsibility, using durable business
state and idempotent delivery. A successful ledger capture alone does not prove
that an item or another external benefit was delivered.

## Schedule funded payments

Schedules store one immutable payment and its recurrence terms. Creation does
not reserve money. Each due occurrence must be funded when its transaction
commits. A schedule cannot mint money or debit an unregistered/closed account.

```java
import com.cmriddles.thenewereconomy.api.schedule.ScheduleRequest;
import java.time.Duration;

var scheduled = new ScheduleRequest(
        "contract:123:payments",
        new Payment(AccountRef.account(fundingAccountUuid), AccountRef.player(recipientUuid),
                "coins", new BigDecimal("5.00")),
        savedFirstDueInstant,
        Duration.ofDays(1),
        30);
schedules.create(scheduled);
```

Persist the request and inspect the returned `ScheduledPayment`. Use
`Duration.ZERO` and `maxOccurrences = 1` for a one-off payment. Recurring intervals
are at least one second; the maximum occurrence count is 1,000,000. Times are
stored to millisecond precision. Reusing a schedule key with changed payment or
recurrence terms fails; cancel the old obligation and create a deliberately new
key if the contract changes.

The worker processes due occurrences in bounded batches. Overdue recurring
occurrences catch up from their original due times; they are not silently skipped.
A server restart or a second worker does not assign a fresh payment key to an
existing occurrence. Each occurrence's saved key and financial transition are
validated inside the ledger transaction.

```java
schedules.find(scheduleId);                   // Optional<ScheduledPayment>
schedules.find("contract:123:payments");
schedules.occurrences(scheduleId, 25, 0);       // newest first, maximum page size 100
schedules.cancel(scheduleId);                 // stops unpaid future occurrences
schedules.resume(scheduleId);                 // after funding/policy recovery
```

`PAUSED_INSUFFICIENT_FUNDS` preserves the unpaid occurrence and its original key.
Fund the source, then explicitly resume. Other operation failures can pause the
schedule as `PAUSED_ERROR`; inspect policy/account state and the occurrence's
message before resuming. Resuming retries the same saved occurrence rather than
creating a replacement payment. `COMPLETED` and `CANCELLED` remain terminal.
Cancellation does not reverse an occurrence that already committed.

All maintenance runs against the configured scope on a work executor. Account
restrictions and integration policies remain effective. Do not use the internal
`tne:` operation-key prefix or `tne.` account-key prefix for your business IDs.
They are reserved for TNE's lifecycle coordination.
