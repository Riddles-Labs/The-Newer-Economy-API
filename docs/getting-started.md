# Your first TNE integration

By the end of this guide, your plugin will read a player's balance and display it
safely on Paper or Folia. Start with a read before adding money movements.

You need a working Java plugin, **JDK 25**, and a **Paper/Folia 26.1+ test server**
with **TNE 1.0.64 or later** installed. You do not need database credentials or
TNECORE access. If this is your first plugin, build and run the
[example](../examples/README.md) first, then follow the code below.

## The four pieces

| Name | Meaning |
| --- | --- |
| `TneApi` | The entry point registered by the running TNE plugin. |
| `EconomyClient` | Your plugin's access to accounts and money operations. |
| `Result<T>` | Either a successful value of type `T`, or a reason the operation failed. |
| `CompletionStage` | Work that finishes later; its callback receives the result. |

The API dependency lets your Java compiler recognize these types. The installed
TNE plugin performs the work and stores the money. Do not copy TNE classes into
your plugin or install the API JAR as a server plugin.

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

The [complete example](../examples/README.md) includes the POM,
balance command, operator account-management commands, plugin metadata,
lifecycle handling, and build instructions.

## Understand the balance example

The service lookup asks Bukkit for the API supplied by TNE. `isAvailable()` is a
readiness hint: storage may still become unavailable after that check, so always
handle the operation's result too.

`api.client(getName())` identifies your plugin in the audit trail. Keep that name
stable across restarts. It is not a player permission check.

`whenComplete` runs after the balance lookup finishes. Check `error` first, then
`result.isSuccess()`. Read `result.value()` only after success. Never use `join()`
or `get()` to wait on a game thread.

The callback can run on a worker thread. `player.getScheduler().run(...)` brings
the player message back onto the player's owning thread. Follow the same rule
for inventory, entity, and world changes.

## Try it on your test server

Build the [example](../examples/README.md), install it alongside TNE, and run
`/tneexamplebalance` as a player. You should see a balance followed by its currency
ID. A newly funded or empty account is a valid result; unavailable storage is a
failure to handle, not a zero balance.

## If something goes wrong

| What you see | What to check |
| --- | --- |
| Your plugin cannot find `TneApi` | Install the normal TNE server JAR and declare the dependency in `plugin.yml`. |
| Your IDE cannot resolve API types | Check the repository URL, version, and Maven/Gradle dependency scope. |
| The API exists but is unavailable | Let TNE finish startup; check its storage logs. Handle the failure and try later. |
| A currency is unavailable | Read `api.currencies()` and use a configured virtual currency ID. |
| A player message causes a thread error | Send it through the player's scheduler, as in the example. |

Next: [create an account and make a payment](payments.md).
For signatures, failure codes, and installation alternatives, use the
[reference](reference.md).
