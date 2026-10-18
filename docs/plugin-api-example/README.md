# TNE native API example

A complete Paper/Folia integration with a player balance command and separately
authorized operator tools for named accounts, funded payments, atomic batches,
receipts, history, and reversals. The API uses the same account and transaction
types for any plugin; it does not depend on a particular game mode.

Additional native service guides in this SDK cover invoices and explicit payer
consent, accepted conversion quotes, physical-note delivery outcomes, filtered
queries, native GUI opens, workflows, delegated access, refunds, and extensions.
Start with [the developer guide](../API.md).

## Build

1. Install **JDK 25** and Maven.
2. In this example's directory (the directory containing its `pom.xml`), run:

   ```shell
   mvn clean package
   ```

   Maven resolves the API from TNE's public Maven repository automatically;
   no manual API installation is required.

3. Download the normal server JAR from
   [TNE 1.0.64](https://github.com/SlyRiddles/TNE-Economy-Releases/releases/tag/v1.0.64).
   Install `target/tne-api-example-1.0.0.jar` alongside
   `TNE-Economy-1.0.64.jar` in your Paper/Folia server's `plugins` directory.
   Start the server and run `/tneexamplebalance` as a player.

As an alternative to repository resolution, use the SDK's API JAR or download
`TNE-API-1.0.64.jar` from that release. In its directory, run the following once
before building the example:

```shell
mvn install:install-file -Dfile=TNE-API-1.0.64.jar -DgroupId=com.cmriddles -DartifactId=the-newer-economy-api -Dversion=1.0.64 -Dpackaging=jar -DgeneratePom=true
```

## Try a named account

`/tneexamplebalance [currency]` is available to players by default and reads only
their own balance. All `/tneexamplepool` tools require `tneexample.pool`, which
defaults to operators. Console use is supported; omit the leading slash there.
Do not grant this permission to ordinary players: the tools deliberately expose
trusted account transfers and account history.

1. Create a stable account:

   ```text
   /tneexamplepool create operating-funds Operating Funds
   ```

   Save the returned account UUID. Repeating this command returns the existing
   account; it does not duplicate the account, rename it, or add money.

2. Have the server operator fund that UUID by transferring existing currency.
   If the currency explicitly allows manual issuance, the operator may use a
   reviewed admin grant instead:

   ```text
   /tneadminadjust grant <account-uuid> coin 1000.00 example-funding:1
   ```

   A grant creates local supply and is blocked under `PRE_FUNDED`. Creating
   an account and using the native payment API never create supply. Check the
   actual currency ID, configured admin command name, and amount first.

3. Inspect the funded account:

   ```text
   /tneexamplepool balance <account-uuid> coin
   /tneexamplepool balances <account-uuid>
   ```

4. Save an operation key in your own order or administrative records, then pay
   an actual player's UUID from that account:

   ```text
   /tneexamplepool pay <account-uuid> <player-uuid> coin 5.00 example-payment:1
   /tneexamplepool receipt example-payment:1
   ```

   The source must have enough money. Keep the same key for recovery after a
   lost response; a different payment needs a different key. An empty receipt
   can mean the original request is still in progress. The example never
   generates a replacement key or automatically retries a financial operation.

## Operator commands

| Command after `/tneexamplepool` | Purpose |
| --- | --- |
| `create <account-key> <display name>` | Create or get a zero-funded account owned by this integration. |
| `find <account-key>` | Recover its UUID using its stable integration-local key. |
| `rename <account-key> <display name>` | Change its name while keeping the identity and balances. |
| `list [offset]` | List 20 integration-owned accounts per page. |
| `balance <account-uuid> [currency]` | Read one currency, defaulting to the configured default. |
| `balances <account-uuid>` | Read all configured virtual-currency balances. |
| `pay <source-account-uuid> <player-uuid> <currency> <amount> <saved-key>` | Pay a player from the chosen account. |
| `charge <destination-account-uuid> <player-uuid> <currency> <amount> <saved-key>` | Debit a player into the chosen account. |
| `transfer <source-account-uuid> <destination-account-uuid> <currency> <amount> <saved-key>` | Move funded money between chosen accounts. |
| `post <saved-key> <source-uuid> <destination-uuid> <currency> <amount> [more groups]` | Commit one or more account payments atomically. |
| `receipt <saved-key>` | Recover this integration's full receipt by its saved operation key. |
| `details <transaction-uuid>` | Read this integration's full signed ledger entries. |
| `history <account-uuid> [offset]` | Read 10 transactions touching the account, across origins. |
| `reverse <transaction-uuid> <saved-key> <reason>` | Append a full compensating reversal of this integration's transaction. |

For an atomic `post`, repeat the four arguments describing each payment. All
UUIDs in these groups are ledger account UUIDs. `pay` and `charge` distinguish
the player's UUID explicitly. No amount is rounded, and no reward/inventory
action is performed by this example.

The example compiles against Paper's published `1.21.8-R0.1-SNAPSHOT` API,
matching TNE's build dependency. Its runtime target is TNE's supported
Paper/Folia 26.1+ server, with Java 25. This compile coordinate is not a promise
that TNE supports older Minecraft servers.

## Adapt it

`/tneexampleapi` adds operator-only examples (`tneexample.api`, default `op`):

| Action | Arguments after `/tneexampleapi` |
| --- | --- |
| Create an invoice | `invoice <account-uuid> <payer-uuid> <currency> <amount> <saved-key> [message]` |
| View or explicitly decide your own invoices | `requests`, `accept <request-uuid> <saved-key>`, `deny <request-uuid>` |
| Cancel this integration's request | `cancel-invoice <request-uuid>` |
| Review an exchange offer, then confirm it | `quote <source> <target> <amount>`, `convert <quote-uuid> <saved-key>` |
| Issue, redeem the held item, or inspect a note | `note <currency> <amount> <saved-key>`, `redeem <saved-key>`, `inspect-note <note-uuid>` |
| Query multiple accounts | `batch <currency> <account-uuid> [more accounts]` |
| Filter recent history by exact metadata | `filter <account-uuid> <currency> <metadata-key> <value> [offset]` |
| Read a leaderboard | `top <currency|all>` |
| Format exact money | `format <currency> <amount> <AMOUNT|NAME|SYMBOL>` |
| Open a real permission-checked TNE menu | `menu <WALLET|EXCHANGE|LEADERBOARD|TRANSACTIONS>` |

Invoice acceptance, exchange confirmation, note actions, and menu opens use the
actual player running the command. They cannot substitute a caller-supplied
player UUID. These tools are examples for operators; your own plugin must obtain
the appropriate player consent before submitting an action.

Check note `delivery()` before treating an issued item as delivered. Stop
automatic retries on `REVIEW_REQUIRED`. See `API_COMMERCE.md` for financial and
inventory outcomes, quote expiry, and bounded query rules.

`mvn test` runs `PaymentRecoveryTest` using `EconomySandbox`, without a Minecraft
server. It demonstrates a lost payment response, receipt recovery after a
simulated restart, and duplicate-key protection. The fixture covers core virtual
money; it does not emulate SQL, physical inventory, exchange quotes, or every
advanced provider capability. Real server integration still needs those checks.

Change the package, main class, plugin name, and command to suit your plugin.
Keep TNE as a `provided` dependency; do not shade its API into your JAR. The
`depend: [TheNewerEconomy]` entry makes TNE's classes available at runtime.
Your plugin name identifies its account and transaction namespace; keep that
name stable after users create accounts or commit transactions.

The commands look up `TneApi` for each request because service availability can
change. TNE starts storage asynchronously, so even a dependent plugin must
check readiness. Balance work is already asynchronous; the callback schedules
player interaction through the player's entity scheduler. If a player retires
before that task runs, no reply is sent. `PoolCommands.complete` handles typed
results once, while `reply` schedules player/console interaction on its owning
thread. Each command stays focused on the relevant API call.

For transfers, decimal amounts, idempotency, audit attribution, and failure
handling, read the API guide: `docs/API.md` in the source checkout or `README.md`
beside the `example` directory in the SDK ZIP.
