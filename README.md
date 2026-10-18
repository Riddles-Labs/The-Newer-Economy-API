# The Newer Economy API

Build your plugin's economy integration with TNE's native accounts, payments,
transaction history, and persistent financial workflows. Use one API for player
wallets, shared accounts, shops, game rewards, settlements, or your own systems.

**Current version: 1.0.64. Requires Java 25 and TNE 1.0.64 or later on Paper/Folia.**
This repository contains the public API, tests, documentation, and an example
plugin. The running TNE plugin supplies the economy implementation.

## Get started

1. Add the API as a compile-time dependency using Maven or Gradle below.
2. Declare `depend: [TheNewerEconomy]` in your `plugin.yml`.
3. Obtain `TneApi` through Bukkit's `ServicesManager` and create a client with
   `api.client(getName())`.
4. Follow the [developer guide](docs/API.md) for working code, asynchronous
   results, threading, idempotency, account creation, and payments.

### Maven

Add this repository and dependency to your existing `pom.xml`:

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

### Gradle Kotlin DSL

```kotlin
repositories {
    maven("https://raw.githubusercontent.com/SlyRiddles/TNE-Economy-Releases/main/maven/")
}

dependencies {
    compileOnly("com.cmriddles:the-newer-economy-api:1.0.64")
}
```

Keep your normal Paper API dependency. Do not shade or relocate the TNE API;
TNE supplies these classes at runtime. The existing public Maven address remains
the artifact host, so integrations can keep their current dependency settings.

## Guides and examples

| What you need | Where to start |
| --- | --- |
| Setup, accounts, balances, transfers, atomic batches, receipts | [Developer guide](docs/API.md) |
| Reserved funds, delivery workflows, scheduled payments | [Durable workflows](docs/API_WORKFLOWS.md) |
| Account controls, delegated budgets, refunds, disputes | [Access and refunds](docs/API_ACCESS.md) |
| Payment requests, currency quotes, money notes, queries, GUI menus | [Commerce and presentation](docs/API_COMMERCE.md) |
| Events, policies, external currency providers, testing | [Extensions and events](docs/API_EXTENSIONS.md) |
| A complete Paper/Folia plugin | [Example project](docs/plugin-api-example/README.md) |

Use `api.capabilities()` to check optional features and inspect every operation's
typed result. Your plugin controls player permissions, consent, and gameplay
delivery. Integration names identify writes for auditing; they do not authenticate
plugins. See the guides for operation-specific guarantees and recovery behavior.

## Downloads

The [TNE 1.0.64 release](https://github.com/SlyRiddles/TNE-Economy-Releases/releases/tag/v1.0.64)
provides the API JAR, source JAR, Javadoc, full developer SDK, and server plugin.
The API JAR is a development dependency; install the server plugin on your server.

## Build from source

Install JDK 25. This project builds independently using the included Maven wrapper:

```shell
./mvnw clean verify
```

On Windows:

```powershell
.\mvnw.cmd clean verify
```

The build runs the API tests and creates the binary, source, and Javadoc JARs in
`target/`. To build the example against the published API:

```shell
./mvnw -f docs/plugin-api-example/pom.xml clean verify
```

`EconomySandbox` supports isolated tests for core account/payment behavior.
Advanced runtime integrations should also be tested on a disposable TNE server.
The current server release remains beta pending live gameplay qualification.

## Issues and contributions

Use this repository's [issue tracker](https://github.com/Riddles-Labs/The-Newer-Economy-API/issues)
for API bugs, documentation problems, and integration requests. Include your API
and server versions, the relevant operation, and a minimal reproducer.
See [CONTRIBUTING.md](CONTRIBUTING.md) for source maintenance and validation.

## License

The API, documentation, and examples in this repository use the [MIT License](LICENSE).
This license applies to this repository's contents; the TNE runtime implementation
is maintained separately.
