# API reference

Use this page while coding. New to the API? Start with
[setup](getting-started.md), then [payments](payments.md).

- [Common operations](#common-operations)
- [Currency IDs and scope](#currency-names-and-scopes)
- [Results and threading](#results-readiness-and-threading)
- [Audit attribution](#what-is-recorded)
- [Manual installation](#manual-api-jar-alternative)
- [Repository layout](#repository-layout)

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

## Repository layout

| Path | What belongs here |
| --- | --- |
| `src/main/java/` | Public contracts, shared value types, and developer test utilities. |
| `src/test/java/` | Tests of those contracts and utilities. They are not packaged in the API JAR. |
| `examples/` | A separate runnable plugin. It is not packaged in the API JAR. |
| `docs/` | Setup, task guides, and this reference. |
| `pom.xml`, `mvnw`, `.mvn/` | Standalone Maven build and wrapper. |
| `.github/workflows/` | Automated API and example verification. |

Start reading code at `TneApi`, then `EconomyClient`, then the service you need.
A small interface or enum is a complete contract, not a placeholder.

Some shared types predate the current client API: `federation/`, the typed IDs in
`asset/`, `payment/`, `transaction/`, and `AccountId`. `security/` contains release
signature verification used by TNE's updater. These shipped in 1.0.64 and remain
for compatibility; their presence does not expose a native federation service.
You do not need them for the account and payment integrations in these guides.

`testing/EconomySandbox` and `CurrencyProviderContract` are optional tools for
writing your own tests. See [testing](API_EXTENSIONS.md#test-without-a-minecraft-server).
