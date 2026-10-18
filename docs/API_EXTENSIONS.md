# Events, policies, external providers, and testing

TNE 1.0.64 adds these services to the existing asynchronous client:

```java
var features = api.capabilities();
if (features.supports("events")) {
    var events = economy.events();
}
var extensions = economy.extensions();
```

Capabilities describe supported interfaces. Readiness is separate: inspect
`api.isAvailable()` and every operation's typed result. The default interface
methods preserve compatibility with older provider implementations; inspect
capabilities before using an optional service. TNE's own 1.0.64 provider implements
the advertised services.

See [the main guide](API.md), [workflows](API_WORKFLOWS.md), and
[account controls and refunds](API_ACCESS.md) for their contracts.

## Durable events

Every newly committed ledger transaction appends its native event in the same
database transaction. This covers other origins such as commands and Vault too.
Account creation, renaming, controls, spending grants, disputes, and external
provider operation records also emit events. Events begin with this release;
historical transactions remain available through history queries.

An event contains a monotonically increasing sequence, type, source namespace,
subject, timestamp, and immutable attributes. Transaction subjects are transaction
UUIDs. Account events use account UUIDs. A full reversal emits
`TRANSACTION_REVERSED`; ordinary commits emit `TRANSACTION_COMMITTED`.

```java
import com.cmriddles.thenewereconomy.api.event.EventFilter;
import java.util.Optional;
import java.util.Set;

var filter = new EventFilter(
    Set.of("TRANSACTION_COMMITTED", "TRANSACTION_REVERSED"),
    Optional.empty(), Optional.of(accountId));

economy.events().read(savedCursor, 100, filter).thenAccept(result -> {
    if (!result.isSuccess()) return;
    var page = result.value();
    // Process page.events() idempotently, then durably save page.nextCursor().
});
```

Limits are 1 through 100; cursors are nonnegative. Filters and server scope apply
before pagination. An account filter matches participating ledger accounts, not
player UUIDs that differ from the ledger account UUID. Resolve a player through
`accounts().find(AccountRef.player(playerUuid))` and use the returned account ID.
Source namespace is the event's actual namespace, rather than its display name.

`subscribe(cursor, filter, listener)` polls on TNE maintenance ticks. A listener
returns `CompletionStage<Void>`. Its in-memory cursor advances after successful
completion; failed or timed-out batches can be delivered again. Processing must
be idempotent. Save the cursor yourself for restart recovery, and close the
returned registration when your plugin stops. Subscription callbacks have a
five-second deadline and execute away from game threads. Use durable polling
instead if processing requires longer. Notification failure never rolls back
committed money.

`availability(listener)` supplies process-local readiness notifications, including
the current status. These are hints; always inspect an operation's result.
Registrations are process-local and must be recreated when your plugin starts.

The event cursor is serialized through database commit. A transaction still
committing cannot later appear below an already delivered cursor. Event listeners
are never invoked while TNE holds financial SQL locks.

## Transaction policies

```java
import com.cmriddles.thenewereconomy.api.extension.TransactionPolicy;
import java.util.concurrent.CompletableFuture;

var registration = economy.extensions().policy("transaction-limit", context -> {
    boolean allowed = context.request().payments().stream()
        .allMatch(payment -> payment.amount().compareTo(limit) <= 0);
    return CompletableFuture.completedFuture(allowed
        ? TransactionPolicy.Decision.allow()
        : TransactionPolicy.Decision.deny("Integration transaction limit exceeded"));
});
```

Policies apply to the registering integration's native payment operations. Stable
policy keys determine evaluation order. A decision can deny the payment or add
explicit funded `Payment` allocations to an ordinary `post` request, supporting
fees, taxes, and recipient splits. Expanded plans remain limited to 100 payments
and undergo normal identity, currency, precision, funding, and ledger validation.

Lifecycle operations such as reservations, delegated payments, refunds, invoice
acceptance, and scheduled payments require exact recorded allocations. Policies
may approve or deny these operations; added fee allocations are rejected. Quote
and lifecycle terms must not change silently during execution. The same exact-plan
rule applies to the existing simple transfer and payout shortcuts.

Callbacks run before SQL financial locks, on a bounded callback executor, with a
250 ms deadline per policy. Failure or timeout prevents the transaction from
starting. They must be fast and free of irreversible side effects. Policies are
process-local and are recreated at startup; persisted spending limits belong in
the [SQL-enforced spending grant service](API_ACCESS.md).

A preflight callback cannot make a read-then-spend check safe against concurrent
payments. TNE rechecks authoritative funding and account restrictions during
commit. Use reservations or spending grants for durable concurrent constraints.
Installed plugins remain trusted code in the same server process.

Close policy registrations on shutdown. At most 16 policies may be registered
per integration.

## External currency providers

`extensions().provider(key, displayName, symbol, scale, capabilities, provider)`
registers a `CurrencyProvider` and returns a `ProviderRegistration`. Its `id()` is
the stable identifier accepted by provider operations. Keep that identifier and
re-register the same integration/key after restart. Closing the registration
removes it from discovery. At most 128 providers may be active.

The provider advertises supported capabilities: balance, debit, credit, transfer,
reserve, capture, release, and recovery. Implement:

- `balance(accountKey)` for an external account's available and reserved amounts.
- `apply(operation)` for a mutation identified by a durable operation key.
- `recover(operationKey)` to query the original operation's outcome.

Account keys are opaque provider-owned strings. A capture or release can use an
external reservation reference as that key. TNE does not treat these identifiers
as Minecraft players or native ledger account IDs.

Use `extensions().providers()` for discovery, `balance(providerId, accountKey)`
for reads, `execute(providerId, operation)` for mutations, and
`recover(providerId, operationKey)` for reconciliation. Mutations require positive
exact amounts within the provider's declared precision and a stable operation
key. Transfer operations require a destination.

Before dispatch, TNE commits an external operation intent. A duplicate request
returns the recorded state without dispatching the effect again; changed terms
under the same key are rejected. Provider failures and lost responses leave a
pending or review-required record. Recovery queries the original key and never
automatically resubmits the effect. Recovered terminal outcomes cannot regress.
A provider must return `REJECTED` only when it authoritatively knows the operation
had no effect and cannot still complete. Timeouts and not-found responses belong
in pending or review-required states.
Callbacks have a three-second deadline; a timeout does not cancel an external
effect already in progress.

The provider must implement durable idempotency and authoritative recovery in its
own storage. An empty recovery result is not permission to retry: the original
operation might still be running or its delivery might be uncertain. Retain the
business record and original key for reconciliation.

Provider balances are separate from TNE's native virtual-currency balances and
are discovered through this service. Registering a provider does not add funds
to TNE, replace configured currencies, or make two independent databases commit
atomically. Combine the provider's explicit reservation/recovery capabilities
with your durable workflow when coordinating external value and native money.

## Test without a Minecraft server

The public API includes `api.testing.EconomySandbox`, a deterministic in-memory
fixture for core account, balance, payment, receipt, history, and reversal tests:

```java
import com.cmriddles.thenewereconomy.api.testing.EconomySandbox;
import java.math.BigDecimal;

var sandbox = new EconomySandbox();
sandbox.seedPlayer(alice, "Alice", "coins", new BigDecimal("100.00"));
sandbox.seedPlayer(bob, "Bob", "coins", BigDecimal.ZERO);
var economy = sandbox.client("MyPlugin");

sandbox.loseNextPaymentReply();
// Call your payment code, then verify recovery using the saved receipt key.
var restarted = sandbox.restart();
```

`failNextPayment(code)` injects a failure before the next financial commit.
`loseNextPaymentReply()` commits a payment but returns an uncertain failure.
`available(false)` simulates unavailable storage; `restart()` retains accounts,
balances, transaction keys, and receipts in a new provider instance. The sandbox
supports a two-decimal `coins` currency and advertises only its supported core
capabilities. Its account UUIDs are synthetic fixture identities.

Test that your integration handles insufficient funds without delivery, duplicate
requests without a second payment, lost responses through receipt recovery,
restart reconciliation, and unavailable storage. TNE's database-backed integration
tests cover the actual lifecycle and concurrency rules. Validate advanced
reservations, workflows, physical delivery, and provider implementations against
a disposable Paper/Folia test server with the storage engines you deploy.

`api.testing.CurrencyProviderContract.debitIdempotency(provider, operation)` is
an asynchronous conformance probe for a **disposable provider fixture**. It needs
BALANCE, DEBIT, and RECOVERY support and a fresh key on an isolated funded test
account. It applies the same debit twice, queries recovery, and checks the balance
reflects exactly one debit. Inspect `Report.passed()` and `violations()`. It leaves
the fixture debited once on success; never run this probe against live player
funds. Its five-second deadline does not cancel an external effect. The probe
accepts a recovered duplicate receipt or a duplicate conflict, followed by an
authoritative matching recovery receipt.
