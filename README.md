# The Newer Economy API

Use TNE to manage money in your Minecraft plugin: read balances, create accounts,
move funds, and keep transaction records. Your plugin defines the gameplay;
TNE supplies the economy and persistent ledger.

**API 1.0.64 · Java 25 · TNE 1.0.64+ · Paper/Folia 26.1+**

## Start here

1. [Set up the API](docs/getting-started.md) and read a player's balance.
2. [Make a payment](docs/payments.md) from an account your plugin uses.
3. [Run the example](examples/README.md) and follow the code in your IDE.

You add the API as a `provided` Maven or `compileOnly` Gradle dependency. The
running TNE server plugin supplies its implementation. You do not need database
credentials, TNECORE credentials, or HTTP calls.

## Find what you need

| Task | Guide |
| --- | --- |
| Install the dependency and read a balance | [Setup](docs/getting-started.md) |
| Accounts, transfers, saved operation keys, receipts, batches | [Payments](docs/payments.md) |
| Reserve funds, coordinate delivery, schedule payments | [Workflows](docs/API_WORKFLOWS.md) |
| Account state, spending budgets, refunds, disputes | [Account controls](docs/API_ACCESS.md) |
| Invoices, conversion quotes, money notes, queries, GUI menus | [Commerce](docs/API_COMMERCE.md) |
| Events, policies, custom providers, integration tests | [Extensions](docs/API_EXTENSIONS.md) |
| Method table, failure codes, threading, shared types | [Reference](docs/reference.md) |

The [example](examples/README.md) includes a balance command, operator account
commands, and a recovery test. Start with the balance command before exploring
the operator tools.

## Downloads and support

Use the [release](https://github.com/SlyRiddles/TNE-Economy-Releases/releases/tag/v1.0.64)
for the server plugin, API JAR, source JAR, Javadoc, and developer SDK.
Maven and Gradle setup is in [setup](docs/getting-started.md#1-add-the-api-dependency).
Existing dependency coordinates and the public Maven URL continue to work.

For help, [open an issue](https://github.com/Riddles-Labs/The-Newer-Economy-API/issues)
with your TNE/API versions, the operation, and a small example that reproduces
the problem. The server release remains beta pending live gameplay qualification.

## Build and contribute

Use JDK 25 and the included Maven wrapper. From the repository root:

```shell
./mvnw clean verify
./mvnw -f examples/pom.xml clean verify
```

On Windows, use `.\mvnw.cmd` in place of `./mvnw`. The first command checks
formatting, runs API tests, and packages the API, sources, and Javadoc in `target/`.
The second builds and tests the example against the published API.
Run `./mvnw spotless:apply` to format Java source before committing.

Keep changes focused, preserve existing contracts, and document new behavior.
API changes need a matching runtime implementation and release review.
The [repository map](docs/reference.md#repository-layout) explains each folder.
Update guides, example dependencies, and version references together for a release;
previously published Maven versions remain immutable.

## License

[MIT](LICENSE), copyright Riddles Labs. This covers the contents of this repository.
The TNE runtime implementation is maintained separately.
