# Invoices, exchange, notes, queries, and native presentation

Use player-approved invoices, saved conversion quotes, physical notes, and native views.

[Setup](getting-started.md) | [Guide index](API.md) | [Payments](payments.md) | [Workflows](API_WORKFLOWS.md)

Use the same `EconomyClient` from the [setup](getting-started.md). These services need
TNE 1.0.64 and Java 25. Every method returns an asynchronous `Result`; check both
exceptional completion and `isSuccess()` before reading its value. Examples show
the individual API call; use the shared completion helper in the example plugin
to handle it without nesting callbacks. Never block a game thread.

## Ask a player to pay an invoice

```java
import com.cmriddles.thenewereconomy.api.request.RequestDraft;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

var draft = new RequestDraft(destinationAccount, payerUuid, "coin",
        new BigDecimal("12.50"), "Invoice 812",
        Optional.of(Instant.now().plusSeconds(600)), "invoice:812");
economy.requests().create(draft);
```

`destinationAccount` is an `AccountRef`. Creation saves a pending request and
**does not debit the payer**. Save the returned request UUID. The same integration
and request key recover the same request; changing its terms under that key is
rejected. Persist the original draft, including its absolute expiry, before
submission. Recomputing `now + 600` on a retry changes the terms. The command
example uses no expiry so repeating the same arguments keeps the same draft.
Account and currency rules still apply. Stored expiries have second precision.

```java
economy.requests().find(savedRequestUuid);        // This integration's request
economy.requests().pending(payerUuid, 20, 0);      // Payer's pending requests

economy.requests().accept(savedRequestUuid, payerUuid, "invoice:812:accept");
economy.requests().deny(savedRequestUuid, payerUuid);
economy.requests().cancel(savedRequestUuid);      // Creating integration only
```

Your plugin must authenticate the player and obtain their explicit consent
before calling `accept`. Passing a UUID is not proof of consent. TNE checks that
this payer owns the debit, the request is still pending and unexpired, and the
currency, scope, amount, and destination still match. Acceptance and money
movement commit atomically. A concurrent cancellation or second acceptance
cannot create an extra payment.

Acceptance returns a full `TransactionReceipt`. Preserve its key and recover
uncertain outcomes with `economy.receipt(key)`. Pending lists are scoped and
paged before returning results; expired requests do not appear as actionable.

## Quote a conversion before confirming it

```java
economy.exchange().quote(playerUuid, "coin", "gems", new BigDecimal("10.00"));
```

Show the returned `ExchangeQuote` to the player: source debit, gross target,
fee, tax, and net target. The configured conversion rules determine the target
precision and rounding; your plugin must not invent a rate or substitute a
rounded estimate for the quote.

After the player confirms that exact offer:

```java
economy.exchange().execute(playerUuid, savedQuote.id(), "conversion:812");
```

A quote expires after 60 seconds and belongs to the requesting integration,
player, and runtime. Changing currency or conversion configuration invalidates
it. Quotes are temporary and do not survive a restart. Request a new quote and
new consent when an offer expires or changes. Use the original operation key
and full receipt lookup to reconcile a conversion already submitted.

Execution uses the displayed amounts, debits the player and funded liquidity,
and commits the exchange and configured fee/tax allocations together. It never
silently replaces the accepted rate with a new one. One quote cannot be charged
again with a different operation key.

## Issue or redeem a physical money note

```java
economy.notes().issue(playerUuid, "coin", new BigDecimal("5.00"), "note:812:issue");
economy.notes().inspect(savedNoteUuid);
economy.notes().redeemHeld(playerUuid, "note:812:redeem");
```

The player must be online and have `tne.money.note`. Issuance debits their balance
into verified backing and creates a signed physical note. It does not create
money. Redemption reads and verifies the actual held item's stored identity;
a note UUID or the inspection DTO alone cannot redeem it.

`inspect` returns stored financial state, not evidence that a player possesses
an item. It verifies the note's original issuance against this server's scope.

A successful `NoteAction` still requires checking `delivery()`:

| Value | Meaning / next action |
| --- | --- |
| `DELIVERED` | The issued item was placed and its delivery recorded. |
| `REDEEMED` | Redemption committed; the signed note cannot pay again. |
| `NOT_DELIVERED` | Backing committed, but the item was definitely not applied. Retry this same issue key after resolving the inventory/player condition. |
| `REVIEW_REQUIRED` | Delivery or cleanup is uncertain. Stop automatic retries and reconcile the note and transaction with the operator. |

`transactionId()` identifies committed financial movement when present.
Inventory changes and a database commit are different effects. TNE durably
claims issuance delivery before touching inventory, so a crash cannot cause a
blind second delivery. A full inventory leaves the issued backing recoverable
under the same key. Do not issue a new key to compensate for a lost response.

Operator recovery must use the dedicated money-note invalidation workflow,
which verifies original backing, invalidates the note, and returns that backing
atomically. Do not reverse or partially refund an issuance through ordinary
transaction APIs: moving backing without invalidating the note would leave a
second claim on the money. Do not reset an uncertain delivery claim manually.

## Batch balance reads and filtered transaction history

```java
economy.queries().balances(List.of(firstAccount, secondAccount), List.of("coin", "gems"));
```

Use up to 100 explicit account references and 100 currency IDs, with at most
1000 requested balances in total. The returned immutable `AccountBalance`
records keep account identity, currency, scope, exact amount, and ledger version.
Missing balances are zero at the configured precision.

```java
import com.cmriddles.thenewereconomy.api.query.HistoryQuery;

var query = new HistoryQuery(account, Optional.of("coin"), Optional.of("MyPlugin"),
        Optional.of(fromInstant), Optional.of(toInstant),
        Map.of("externalReference", "distribution-42"), 20, 0);
economy.queries().history(query);
```

Time bounds are inclusive at `from` and exclusive at `to`. The integration
filter is the exact declared native integration name. Omit it to include other
origins touching the account. Metadata filters are exact key/value matches.
All filters precede the returned logical page; `limit` is 1..100.

Account, scope, currency, time, and integration are filtered in SQL. Metadata
uses exact decoding for compatibility with historical JSON and escaped-map
records. This bounded scan allows 10,000 already-scoped transactions; if a
complete page cannot be determined within that budget, it returns a typed
failure asking for a narrower range, not a misleading partial page.

```java
economy.queries().leaderboard("coin", 20, 0);
economy.queries().leaderboard("all", 20, 0); // Overall average percentile score
```

Overall scores are ranks, not summed or averaged money. Use `currency=all` if a
literal currency is named `all`. Account privacy and player permissions remain
your plugin's responsibility when displaying trusted query results.

## Format money and open existing menus

```java
import com.cmriddles.thenewereconomy.api.presentation.MoneyStyle;
import com.cmriddles.thenewereconomy.api.presentation.MenuView;

economy.presentation().format("coin", new BigDecimal("12.50"), MoneyStyle.NAME);
economy.presentation().format("coin", new BigDecimal("12.50"), MoneyStyle.SYMBOL);
economy.presentation().open(playerUuid, MenuView.WALLET);
```

`AMOUNT` gives exact decimal text; `NAME` adds the configured singular/plural
name; `SYMBOL` follows the configured symbol, position, and grouping. Formatting
returns plain text, respects precision, and rejects unsupported fractions
instead of rounding them. Construct money from decimal strings.

Native menu views are `WALLET`, `EXCHANGE`, `LEADERBOARD`, and `TRANSACTIONS`.
Opening schedules the existing menu on the player entity's owning thread and
checks their current permissions. Its result completes after opening or a
rejection. No command is executed, and opening a menu does not authorize a
financial operation. Your own callback still needs the appropriate Paper/Folia
scheduler before touching player or world state.
