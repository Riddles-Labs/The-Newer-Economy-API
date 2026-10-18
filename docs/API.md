# TNE API for plugin developers

Use TNE as the persistent economy backend for your plugin. Your plugin decides
what an account represents, who may use it, and when money should move. TNE saves
accounts, enforces currency and funding rules, commits transactions atomically,
and keeps the audit trail. The same account and transaction interfaces support
personal purses, shared banks, temporary game funds, purchases, and settlements.
You do not need TNECORE credentials, HTTP requests, database access, or TNE commands.

The entry point is `TneApi`. Get it from Bukkit's `ServicesManager`, then create
one client using your plugin's name. Balance and money operations are
asynchronous and return a typed result.

Requires **TNE 1.0.64 or later and Java 25**. This guide covers the native
Paper/Folia API. The TNECORE backend HTTP API is separate from this plugin API.

Choose the service that matches the work your plugin needs:

| Need | API / guide |
| --- | --- |
| Named accounts, balances, payments, atomic batches | Start with the quick start below. |
| Invoices, conversion quotes, physical notes, filtered reads, native GUIs | [Commerce, queries, and presentation](API_COMMERCE.md). |
| Reserve funds, manage workflows, or schedule payments | [Durable workflows](API_WORKFLOWS.md). |
| Account state, delegated budgets, refunds, and disputes | [Account access and refunds](API_ACCESS.md). |
| Custom providers, policy checks, events, and integration testing | [Extensions and events](API_EXTENSIONS.md). |

An installed provider can advertise features through
`api.capabilities().supports("requests")` or another service name. Feature
availability is separate from `api.isAvailable()`, which reports runtime
readiness. TNE 1.0.64 exposes the services listed above.

## 1. Add the API dependency

For Maven, add TNE's public repository and API dependency to your plugin's
`pom.xml`. Merge these entries into any existing `repositories` and
`dependencies` sections; keep your Paper repository and dependency too:

```xml
<repositories>
    <repository>
        <id>tne-public</id>
        <url>https://raw.githubusercontent.com/SlyRiddles/TNE-Economy-Releases/main/maven/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.cmriddles</groupId>
        <artifactId>the-newer-economy-api</artifactId>
        <version>1.0.64</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Use `provided` and **do not shade or relocate the TNE API** into your plugin.
TNE provides these classes at runtime; a second copy can prevent service lookup.
The public repository also supplies sources and Javadoc for your IDE.

For Gradle, add the repository and `compileOnly` dependency to `build.gradle.kts`:

```kotlin
repositories {
    maven("https://raw.githubusercontent.com/SlyRiddles/TNE-Economy-Releases/main/maven/")
}

dependencies {
    compileOnly("com.cmriddles:the-newer-economy-api:1.0.64")
}
```

Keep your normal Paper API dependency too. The TNE JAR belongs on the compile
classpath only; install the normal `TNE-Economy-1.0.64.jar` on your server.

### Manual API JAR alternative

You can also download `TNE-API-1.0.64.jar` or the complete
`TNE-Developer-SDK-1.0.64.zip` from
[the 1.0.64 release](https://github.com/SlyRiddles/TNE-Economy-Releases/releases/tag/v1.0.64).
The SDK includes sources, Javadoc, and a complete example plugin.

If you prefer a local API dependency, install the downloaded JAR into your local
Maven repository, then use the same `provided` dependency shown above:

```shell
mvn install:install-file -Dfile=TNE-API-1.0.64.jar -DgroupId=com.cmriddles -DartifactId=the-newer-economy-api -Dversion=1.0.64 -Dpackaging=jar -DgeneratePom=true
```

For Gradle's local-file alternative, copy it to `libs` and replace the coordinate
dependency with `compileOnly(files("libs/TNE-API-1.0.64.jar"))`. Your CI must also
receive that file or perform the same local installation step.

## 2. Declare TNE as a dependency

In `plugin.yml`:

```yaml
name: MyPlugin
version: 1.0.0
main: example.MyPlugin
api-version: "26.1"
folia-supported: true
depend: [TheNewerEconomy]
```

`depend` makes TNE load before your plugin and makes the API classes visible.
It does not guarantee that TNE's asynchronous storage initialization has finished.
Check availability when you handle a request. Only declare `folia-supported` if
your whole plugin follows Folia's threading rules.

If your plugin uses `paper-plugin.yml` instead, declare its server dependency:

```yaml
dependencies:
  server:
    TheNewerEconomy:
      load: BEFORE
      required: true
      join-classpath: true
```

Here `BEFORE` means TNE must load before your plugin, and `join-classpath` gives
your plugin access to TNE's API classes. See Paper's
[dependency documentation](https://docs.papermc.io/paper/dev/getting-started/paper-plugins/)
and [plugin interoperability reference](https://docs.papermc.io/paper/reference/paper-plugins/).
The complete example uses `plugin.yml`; you do not need both descriptors.

## 3. Get the API and read a balance

Imports used below:

```java
import com.cmriddles.thenewereconomy.api.EconomyClient;
import com.cmriddles.thenewereconomy.api.TneApi;
import java.math.BigDecimal;
```

Inside your `JavaPlugin`, on the player's owning thread:

```java
TneApi api = getServer().getServicesManager().load(TneApi.class);
if (api == null || !api.isAvailable()) {
    player.sendMessage("The economy is starting or unavailable. Please try again shortly.");
    return;
}

EconomyClient economy = api.client(getName());
economy.balance(player.getUniqueId()).whenComplete((result, error) -> {
    if (!isEnabled()) return;
    // TNE completes asynchronously. Schedule player interaction on their owning thread.
    player.getScheduler().run(this, task -> {
        if (error != null) {
            player.sendMessage("Unable to read your balance. Please try again shortly.");
        } else if (!result.isSuccess()) {
            player.sendMessage("Unable to read your balance: " + result.failure().code());
        } else {
            var balance = result.value();
            player.sendMessage(balance.amount().toPlainString() + " " + balance.currency());
        }
    }, null);
});
```

`balance(uuid)` uses the configured default currency. Use
`balance(uuid, "coin")` to select a currency by its ID. UUIDs work for offline
players too; looking up a username is your plugin's responsibility.

The [complete example plugin](plugin-api-example/README.md) includes the POM,
balance command, operator account-management commands, plugin metadata,
lifecycle handling, and build instructions.

## 4. Create a named account and choose where money goes

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

## 5. Handle a payment result

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

## Atomic transactions, receipts, and history

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

## Common operations

All methods below belong to `EconomyClient`. Default-currency overloads omit
the `currency` argument.

| Task | Call | Successful value |
| --- | --- | --- |
| Create or get a named account | `accounts().create(accountKey, displayName)` | `Account` |
| List this integration's named accounts | `accounts().list(limit, offset)` | `List<Account>` |
| Look up a named account by its stable key | `accounts().find(accountKey)` | `Optional<Account>` |
| Look up an account reference | `accounts().find(accountRef)` | `Optional<Account>` |
| Rename a named account | `accounts().rename(accountKey, displayName)` | `Account` |
| Read a chosen account | `balance(accountRef, currency)` | `AccountBalance` |
| Read all virtual balances of an account | `balances(accountRef)` | `List<AccountBalance>` |
| Pay from a chosen account | `pay(sourceRef, playerUuid, currency, amount, key)` | `Transaction` |
| Charge into a chosen account | `charge(playerUuid, destinationRef, currency, amount, key)` | `Transaction` |
| Move money between chosen accounts | `transfer(sourceRef, destinationRef, currency, amount, key)` | `Transaction` |
| Read default balance | `balance(playerUuid)` | `Balance` |
| Read selected balance | `balance(playerUuid, currency)` | `Balance` |
| Pay another player | `transfer(fromUuid, toUuid, currency, amount, key)` | `Transaction` |
| Credit a player | `deposit(playerUuid, currency, amount, key)` | `Transaction` |
| Debit a player | `withdraw(playerUuid, currency, amount, key)` | `Transaction` |
| Post an atomic set of payments | `post(request)` | `TransactionReceipt` |
| Recover a full receipt by saved key | `receipt(key)` | `Optional<TransactionReceipt>` |
| Read this integration's full receipt by UUID | `receipt(transactionUuid)` | `Optional<TransactionReceipt>` |
| Read account history across transaction origins | `history(accountRef, limit, offset)` | `List<TransactionReceipt>` |
| Reverse one of this integration's transactions | `reverse(transactionUuid, reason, key)` | `TransactionReceipt` |
| Read the original single-payment summary | `transaction(key)` | `Optional<Transaction>` |

`amount` is a positive `BigDecimal`. Currency precision is enforced; the API
does not silently round an invalid amount. Read `CurrencyInfo.scale()` to learn
how many decimal places a currency supports.

A `Balance` is a snapshot, not a reservation. Decide whether a payment succeeded
from the transfer or withdrawal result, rather than checking a balance first.
`Transaction.amount()` is the amount moved, not the player's resulting balance.

The original `deposit` and `withdraw` methods are shortcuts for the configured
Vault payout account: a deposit debits it and credits the player; a withdrawal
does the reverse. Use `pay` and `charge` when you want your own shop, rewards,
or treasury account instead. These are balanced, audited ledger transactions,
not unbacked money creation.

The server operator trusts installed plugins with this access; the API does not
automatically check a player's command permissions. Your plugin must authorize
its own actions. Never expose an unrestricted player-controlled deposit command.

### Configured payout-pool shortcuts

The account UUID is configured in `economy.yml` under
`integrations.vault.settlement-account-id` (default:
`00000000-0000-0000-0000-000000000001`). Native deposits use that same account
identity for `deposit` and `withdraw`, with a separate balance for each currency
in the configured scope. Explicit `pay` and `charge` use the account you select.
Do not assume that funding the default currency funds every other currency.

The owner can open **Server console > your server > Funding setup** on the
website, select the currency, and transfer existing Treasury funds to the
**Vault settlement** account. If local manual issuance is explicitly allowed,
an administrator with `tne.admin.adjust` can alternatively issue a reviewed
amount to that configured account:

```text
/tneadminadjust grant <configured-payout-account-uuid> coin 1000.00 native-api-funding:coin:1
```

Replace the UUID, currency, amount, and unique operation key deliberately; the
command name can be changed in configuration. A grant creates local currency
supply and remains subject to the currency's supply policy; `PRE_FUNDED` blocks
issuance. It is not needed if existing funds can be transferred. See
[owner funding controls](https://github.com/SlyRiddles/TNE-Economy-Releases/blob/main/SERVER_OWNER_CONTROLS.md#guided-funding-setup-tne-economy-1020--tnecore-1022)
for the funding review and policy details. Funding a Rewards account alone does
not fund this configured payout account or a named account you created.

## Currency names and scopes

```java
var defaultCurrency = api.defaultCurrency();
String id = defaultCurrency.id();             // e.g. "coin": use in API calls
String display = defaultCurrency.pluralName(); // e.g. "Coins": use in your UI
int decimals = defaultCurrency.scale();

for (var currency : api.currencies()) {
    // Configured virtual currencies usable through this API.
}
```

Currency IDs are case-insensitive and normalize to lowercase. Display names are
separate, with initial capitals. Do not use a display name, symbol, or formatted
balance as an identifier. Virtual-money operations do not deliver item currencies or XP. Use
`notes()` for signed physical money notes; see the commerce guide.

The client uses `api.defaultScope()`, TNE's configured server scope. `scope()`
shows that value. There is no scope override in this API; this is not a
remote-server or federated settlement API.

## Results, readiness, and threading

Money and balance methods return `CompletionStage<Result<T>>`:

1. Check an exceptional completion (`error != null`). Unexpected failures and
   shutdown can happen. An exception is not proof that a write did not commit.
2. Otherwise check `result.isSuccess()`.
3. Read `result.value()` only on success. Read `result.failure()` only on failure.

Useful failure codes:

| Code | Meaning / next step |
| --- | --- |
| `VALIDATION_ERROR` | Check UUIDs, positive amount, precision, or key. |
| `CURRENCY_UNAVAILABLE` | Check the currency ID and configured currency type. |
| `INSUFFICIENT_FUNDS` | The required debit cannot be funded. |
| `ACCOUNT_RESTRICTED` | An account policy prevents the operation. |
| `PERMISSION_DENIED` | TNE policy rejected the operation. |
| `CONFLICT` | Conflicting/repeated operation; reconcile using your saved records. |
| `DEPENDENCY_UNAVAILABLE` | Storage/runtime is starting, stopping, or unavailable. |
| `INTERNAL_ERROR` | Record the failure's correlation ID for investigation. |

The failure also provides `message()` and `correlationId()`. Log these for
diagnosis; keep player-facing messages short. Do not parse English error messages
to decide whether a transaction succeeded. Invalid operation inputs return a
validation result; `client(integrationName)` itself rejects blank, oversized, or
control-character-containing names synchronously.

`isAvailable()` is a quick readiness check, not a guarantee that the next request
will succeed. Currency metadata is cached and can be read without waiting for
storage. The provider is unregistered when TNE stops. Look up the service again
after a lifecycle change; treat an old client as unavailable.

Do not call `.join()` or blocking `.get()` on Paper's main thread or any Folia
global, region, or entity thread. The API schedules its own storage work; you can
call it from a command handler without creating your own database thread.
Completion callbacks can run on a worker thread. Use the relevant Paper/Folia
scheduler for inventory changes, messages, entities, and world access. The example
uses `player.getScheduler().run(...)`, which also handles the player retiring.

## What is recorded

Create your client with `api.client(getName())` so writes are attributed to your
declared integration in TNE's transaction and audit records. This is attribution,
not plugin authentication. Use a stable plugin name; do not put player input or
an order ID in the integration name. The per-operation idempotency key identifies
the individual action.

Writes go through TNE's existing transaction engine, balance checks, immutable
double-entry ledger, audit records, and configured storage. This API does not
provide a balance setter or direct SQL access. It does not require your plugin
to construct ledger entries, balancing UUIDs, storage services, or commands.

The API classes to depend on are under `com.cmriddles.thenewereconomy.api`.
Platform and application implementation classes are internal integration details;
do not cast the provider or depend on `Paper*` classes.
