# Accounts and payments

Start with [setup](getting-started.md). The examples here assume an available
`EconomyClient economy`. They introduce the same account and payment model used
for rewards, shops, shared funds, and other plugin features.

## Before moving money

| Concept | Rule |
| --- | --- |
| Account | Holds a separate balance for each configured currency. Creating it does not create money. |
| Player UUID | Identifies a Minecraft player. Use `AccountRef.player(uuid)`. |
| Account UUID | Identifies a ledger account. Use `AccountRef.account(uuid)`. |
| Currency ID | A configured identifier such as `coin`; a display name such as `Coins` is not an ID. |
| Operation key | A saved identifier for one business action, reused when recovering that same action. |

Your plugin decides who is allowed to pay and what successful payment buys. TNE
checks and records the money movement. Save the operation's participants, amount,
currency, and key before submitting it.

## 1. Create an account

Create a pool once using a stable key belonging to your integration:

```java
economy.accounts().create("operating-funds", "Operating Funds").thenAccept(result -> {
    if (!result.isSuccess()) {
        // Inspect result.failure(); account creation failed.
        return;
    }
    var account = result.value();
    // Save account.id(), or find it later using this same integration and key.
});
```

Calling `accounts().create` again with the same integration and account key returns
the original account, including its original display name. This makes startup
registration safe to repeat. It creates an empty account, **not money**. Each
currency has its own balance within that account in the configured scope.
Keep account keys stable across plugin versions and restarts.

Account keys are case-insensitive and normalize to lowercase. They are 1 to 64
ASCII letters, digits, dots, underscores, colons, or hyphens, and must start with
a letter or digit. Names are 1 to 64 characters with no control characters. Look up or
rename a known account without creating another one:

```java
economy.accounts().find("operating-funds");
economy.accounts().rename("operating-funds", "Application Funds");
economy.accounts().list(20, 0); // This integration's accounts; limit is 1..100
```

Renaming changes the readable name, not the UUID, key, balances, or historical
ledger entries. `Account.type()` distinguishes player, system, and plugin
accounts; `state()` reports active, restricted, locked, or closed status.

After the operator funds that account, pay a player from it:

```java
import com.cmriddles.thenewereconomy.api.account.AccountRef;

var rewards = AccountRef.account(savedRewardsAccountUuid);
economy.pay(rewards, playerUuid, "coin", new BigDecimal("5.00"), "quest:104:reward");
```

Charge a player into your shop account:

```java
var shop = AccountRef.account(savedShopAccountUuid);
economy.charge(playerUuid, shop, "coin", new BigDecimal("12.50"), "order:812:payment");
```

Move money between two chosen accounts and read their balances:

```java
var treasury = AccountRef.account(savedTreasuryAccountUuid);
economy.transfer(treasury, rewards, "coin", new BigDecimal("100.00"), "rewards:fund:7");
economy.balance(rewards, "coin"); // One currency's AccountBalance
economy.balances(rewards);        // All configured virtual currencies
economy.accounts().list(20, 0);   // This integration's accounts, first page
```

These short calls return asynchronous results; handle each result before
continuing your business action, as shown in the next section. None of these
operations mint money. A debit fails if the selected source lacks funds.

`AccountRef.player(uuid)` identifies a player; `AccountRef.account(uuid)`
identifies a ledger account. Keep the distinction explicit. Existing overloads
such as `balance(playerUuid)` and `transfer(fromPlayerUuid, toPlayerUuid, ...)`
continue to mean player UUIDs. A named account's `ref()` gives you its ledger
reference directly.

### Fund a chosen account

Create the account, copy its returned UUID, and give that UUID to the server
operator. The operator can fund it by transferring existing currency into it,
using an authorized account transfer or the website's balance/account controls.
If the currency allows manual issuance, a reviewed administrative grant can
target that UUID:

```text
/tneadminadjust grant <named-account-uuid> coin 1000.00 quest-rewards-funding:1
```

This grant is an operator action that creates local currency supply, subject
to supply policy; `PRE_FUNDED` blocks issuance. Your plugin's account creation,
`pay`, `charge`, `transfer`, and `post` never create supply. Funding one currency does
not fund every currency in the account. Use a fresh funding key for a different
funding operation, and preserve the original key when recovering the same one.

The complete example provides operator-only commands to create and inspect a
pool, then pay, charge, or transfer using explicit account UUIDs and saved keys.

## 2. Submit and handle a payment

Use a decimal string to create money. `new BigDecimal("12.50")` is exact; avoid
`new BigDecimal(12.50)` and converting money through `double`.

```java
// Save this ID with your business operation BEFORE submitting the transfer.
String operationKey = "order:" + persistedOrderId;

economy.transfer(buyerUuid, sellerUuid, "coin", new BigDecimal("12.50"), operationKey)
        .whenComplete((result, error) -> {
            if (error != null) {
                // Outcome may be unknown. Reconcile the operation; do not invent a new key.
                return;
            }
            if (!result.isSuccess()) {
                // Inspect result.failure().code(); do not grant the purchased item here.
                return;
            }
            var transaction = result.value();
            // Persist transaction.transactionId() against the order.
            // Deliver any reward exactly once, on its correct Paper/Folia thread.
        });
```

This fragment demonstrates the money operation; it is not a complete shop.
Your plugin must persist order state and make reward delivery idempotent.
A successful transfer does not make an inventory edit atomic with the ledger.
Disconnects or a server restart can happen between those two actions.

Every money mutation requires an **idempotency key**. Use one unique, persistent key for
each logical operation. Reuse that same key only when recovering that exact
operation with the same players, currency, amount, and scope. A retry does not
guarantee a second `SUCCESS` response: it can return `CONFLICT`. Do not assume
`CONFLICT` proves your specific operation succeeded, and do not create a fresh
key to bypass it. Keep your own durable operation and transaction records for
reconciliation. Two independent purchases must use different keys.
Keys share one namespace per integration across all currencies and mutation
types, so do not reuse an order's payment key for its later refund.
Keys contain 1 to 128 ASCII letters, digits, dots, underscores, colons or hyphens;
spaces are invalid. A persisted order ID or UUID with a short operation prefix
works well, such as `order:123:payment`.

### Recover a lost response

Use the original integration client and saved key to look up its committed
receipt. This reads the ledger; it does not submit a second payment:

```java
economy.receipt(operationKey).whenComplete((result, error) -> {
    if (error != null || !result.isSuccess()) {
        // Keep the order pending and try reconciliation again later.
        return;
    }
    var receipt = result.value(); // Optional<TransactionReceipt>
    if (receipt.isPresent()) {
        var transaction = receipt.orElseThrow();
        // Match this to your saved operation; record transaction.transactionId().
        // Resume your own exactly-once delivery workflow.
    } else {
        // No committed receipt was visible. The original request may still be in flight.
        // Keep the saved key; do not invent a replacement key or assume failure.
    }
});
```

`receipt(key)` searches only this integration's namespace and supports single
payments and atomic batches. A successful
empty result means no committed receipt was found at the time of the read; it
does not prove the original request failed. A `CONFLICT` on a write is also not
proof that its arguments match your intended operation. Your durable order
record remains the source of the players, amount, currency, and intended action.

## 3. Group payments and inspect transactions

The same account and transaction abstractions work for any plugin. A named
account can represent an organization's funds, an application's operating
balance, a settlement destination, or another purpose your plugin defines.
TNE does not require a special API for each game mode or plugin type.

For one payment, use `transfer`, `pay`, or `charge`. For several payments that
must commit together, use `post`:

```java
import com.cmriddles.thenewereconomy.api.economy.Payment;
import com.cmriddles.thenewereconomy.api.economy.TransactionRequest;
import java.util.List;
import java.util.Map;

var request = new TransactionRequest(List.of(
        new Payment(source, firstDestination, "coin", new BigDecimal("90.00")),
        new Payment(source, secondDestination, "coin", new BigDecimal("10.00"))
), "distribution:42", Map.of("externalReference", "distribution-42"));

economy.post(request).whenComplete((result, error) -> {
    if (error != null || !result.isSuccess()) {
        // Keep your saved operation; reconcile an uncertain outcome by its original key.
        return;
    }
    var receipt = result.value();
    // All payments committed atomically. Save receipt.transactionId().
});
```

Here `source`, `firstDestination`, and `secondDestination` are `AccountRef`s.
Each `Payment` declares its funded debit and matching credit; you do not build
unbalanced ledger entries. TNE validates the whole request and commits all its
payments together or none. Do not replace an atomic batch with several separate
transfers when partial completion would break your business operation.

Funding is checked on the **net change for each account and currency** across
the entire batch. Incoming payments in that same atomic transaction can fund
outgoing payments; the resulting balance cannot be negative. One currency's
incoming funds cannot cover another currency's debit. Balances remain in the
configured scope, and issuance accounts cannot be used to bypass funding rules.

One request accepts up to 100 payments. Custom metadata accepts up to 16 entries,
with keys up to 64 characters and values up to 512 characters. System,
integration-attribution, and reversal metadata keys are reserved. DTO constructors
validate their shape and can throw `IllegalArgumentException`; handle invalid
request construction before submission. Do not put secrets in financial metadata.

`TransactionReceipt.entries()` contains the actual signed ledger entries:
negative amounts are debits and positive amounts are credits. Each entry names
its account, currency, and scope. Receipts include the transaction UUID, creation
time, correlation UUID, integration source, key, and metadata. Account names are
resolved for the read and may reflect a later rename; the ledger account UUID
is the stable identity.

```java
economy.receipt(savedTransactionUuid); // This integration's full receipt
economy.receipt(savedOperationKey);    // Recovery by this integration's saved key
economy.history(accountRef, 20, 0);    // First 20 transactions touching this account
```

History can include transactions from other integrations or administrative
actions; it is not restricted to your own writes. This is a trusted server API,
so authorize access before showing account history to a player. Pagination uses
`limit` and `offset`; request another page instead of loading unbounded history.

The original `transaction(key)` method returns a small single-payment summary.
It cannot summarize an atomic batch and returns a validation failure for one.
Use `receipt(key)` for general transaction recovery and auditing.

### Compensating reversals

```java
economy.reverse(savedTransactionUuid, "Incorrect distribution", "distribution:42:reversal");
```

This creates a new transaction reversing all entries of one of your integration's
transactions. It keeps the original immutable, applies the normal account and
funding checks, and allows one full reversal of that original transaction.
It can fail if the accounts that must return money no longer have enough.
The supplied key belongs to the new reversal; preserve it for response recovery.
Reversals of another integration's transaction are not permitted by this method.
Do not use it as a substitute for reconciling an uncertain original operation.

## Where to go next

Use the [reference](reference.md) for a method table and failure codes.
Use [workflows](API_WORKFLOWS.md) when payment and delivery span multiple steps,
[commerce](API_COMMERCE.md) for invoices and conversion, or
[account controls](API_ACCESS.md) for budgets and refunds.
