package example;

import com.cmriddles.thenewereconomy.api.EconomyClient;
import com.cmriddles.thenewereconomy.api.TneApi;
import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.economy.Payment;
import com.cmriddles.thenewereconomy.api.economy.TransactionRequest;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;

/** Operator-only account tools. Every payment requires an explicitly supplied, saved key. */
final class PoolCommands {
    private final JavaPlugin plugin;

    PoolCommands(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void register() {
        Objects.requireNonNull(plugin.getCommand("tneexamplepool")).setExecutor(this::execute);
    }

    private boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tneexample.pool")) {
            sender.sendMessage("You do not have permission to manage example accounts.");
            return true;
        }
        if (!(sender instanceof Player) && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("Use these tools as a player or from the server console.");
            return true;
        }
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        TneApi api = plugin.getServer().getServicesManager().load(TneApi.class);
        if (api == null || !api.isAvailable()) {
            reply(sender, "The economy is starting or unavailable. Try again shortly.");
            return true;
        }

        EconomyClient economy = api.client(plugin.getName());
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create" -> {
                    if (args.length < 3) { usage(sender); break; }
                    String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    complete(sender, economy.accounts().create(args[1], name),
                            account -> "Account: " + account.name() + " | UUID: " + account.id()
                                    + " | Creating an account does not fund it.");
                }
                case "list" -> {
                    if (args.length > 2) { usage(sender); break; }
                    complete(sender, economy.accounts().list(20, args.length == 2 ? Integer.parseInt(args[1]) : 0), accounts -> accounts.isEmpty()
                            ? "This integration has no named accounts."
                            : accounts.stream().map(account -> account.id() + " | " + account.name())
                                    .collect(Collectors.joining("\n")));
                }
                case "find" -> {
                    if (args.length != 2) { usage(sender); break; }
                    complete(sender, economy.accounts().find(args[1]), account -> account
                            .map(value -> "Account: " + value.name() + " | UUID: " + value.id())
                            .orElse("No account exists for that key in this integration."));
                }
                case "rename" -> {
                    if (args.length < 3) { usage(sender); break; }
                    String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    complete(sender, economy.accounts().rename(args[1], name),
                            account -> "Account renamed: " + account.name() + " | UUID: " + account.id());
                }
                case "balance" -> {
                    if (args.length < 2 || args.length > 3) { usage(sender); break; }
                    String currency = args.length == 3 ? args[2] : economy.defaultCurrency().id();
                    complete(sender, economy.balance(account(args[1]), currency),
                            balance -> balance.amount().toPlainString() + " " + balance.currency());
                }
                case "balances" -> {
                    if (args.length != 2) { usage(sender); break; }
                    complete(sender, economy.balances(account(args[1])), balances -> balances.stream()
                            .map(balance -> balance.amount().toPlainString() + " " + balance.currency())
                            .collect(Collectors.joining("\n")));
                }
                case "pay", "charge", "transfer" -> {
                    if (args.length != 6) { usage(sender); break; }
                    AccountRef pool = account(args[1]);
                    UUID target = UUID.fromString(args[2]);
                    BigDecimal amount = new BigDecimal(args[4]);
                    // The operator supplies a key saved with this one business operation.
                    // Never generate a replacement key automatically after an uncertain result.
                    var operation = switch (args[0].toLowerCase(Locale.ROOT)) {
                        case "pay" -> economy.pay(pool, target, args[3], amount, args[5]);
                        case "charge" -> economy.charge(target, pool, args[3], amount, args[5]);
                        default -> economy.transfer(pool, AccountRef.account(target), args[3], amount, args[5]);
                    };
                    complete(sender, operation, receipt -> "Committed transaction: " + receipt.transactionId()
                            + " | " + receipt.amount().toPlainString() + " " + receipt.currency());
                }
                case "receipt" -> {
                    if (args.length != 2) { usage(sender); break; }
                    complete(sender, economy.receipt(args[1]), receipt -> receipt
                            .map(value -> "Committed transaction: " + value.transactionId()
                                    + " | " + value.entries().size() + " ledger entries")
                            .orElse("No committed receipt is visible. It may still be in flight; keep the same key."));
                }
                case "post" -> {
                    // Each group contains source UUID, destination UUID, currency, amount.
                    if (args.length < 6 || (args.length - 2) % 4 != 0) { usage(sender); break; }
                    var payments = new ArrayList<Payment>();
                    for (int index = 2; index < args.length; index += 4) {
                        payments.add(new Payment(account(args[index]), account(args[index + 1]),
                                args[index + 2], new BigDecimal(args[index + 3])));
                    }
                    complete(sender, economy.post(new TransactionRequest(payments, args[1])),
                            receipt -> "Committed atomic transaction: " + receipt.transactionId()
                                    + " | " + receipt.entries().size() + " ledger entries");
                }
                case "details" -> {
                    if (args.length != 2) { usage(sender); break; }
                    complete(sender, economy.receipt(UUID.fromString(args[1])), receipt -> receipt
                            .map(value -> "Transaction: " + value.transactionId() + "\n"
                                    + value.entries().stream()
                                            .map(entry -> entry.accountName() + " | " + entry.account().id()
                                                    + " | " + entry.amount().toPlainString() + " " + entry.currency())
                                            .collect(Collectors.joining("\n")))
                            .orElse("No receipt found in this integration's namespace."));
                }
                case "history" -> {
                    if (args.length < 2 || args.length > 3) { usage(sender); break; }
                    int offset = args.length == 3 ? Integer.parseInt(args[2]) : 0;
                    complete(sender, economy.history(account(args[1]), 10, offset), history -> history.isEmpty()
                            ? "No transactions on this history page."
                            : history.stream().map(receipt -> receipt.transactionId() + " | " + receipt.createdAt()
                                    + " | " + receipt.integrationName()).collect(Collectors.joining("\n")));
                }
                case "reverse" -> {
                    if (args.length < 4) { usage(sender); break; }
                    String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    complete(sender, economy.reverse(UUID.fromString(args[1]), reason, args[2]),
                            receipt -> "Committed compensating reversal: " + receipt.transactionId());
                }
                default -> usage(sender);
            }
        } catch (IllegalArgumentException invalid) {
            reply(sender, "Invalid input: " + invalid.getMessage());
        }
        return true;
    }

    private static AccountRef account(String uuid) {
        return AccountRef.account(UUID.fromString(uuid));
    }

    <T> void complete(CommandSender sender, CompletionStage<Result<T>> stage, Function<T, String> display) {
        stage.whenComplete((result, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.WARNING, "TNE example operation failed; reconcile writes using their saved key", error);
                reply(sender, "Request failed. A write may have committed; use receipt with its saved key.");
            } else if (!result.isSuccess()) {
                var failure = result.failure();
                plugin.getLogger().warning("TNE example: " + failure.code() + " | " + failure.message()
                        + " | reference=" + failure.correlationId());
                reply(sender, failure.code() + ": " + failure.message()
                        + " | Keep your original key for reconciliation.");
            } else {
                // This function formats immutable API values; it must not touch Bukkit state.
                reply(sender, display.apply(result.value()));
            }
        });
    }

    void reply(CommandSender sender, String message) {
        if (!plugin.isEnabled()) return;
        try {
            if (sender instanceof Player player) {
                player.getScheduler().run(plugin, task -> player.sendMessage(message), null);
            } else {
                plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> sender.sendMessage(message));
            }
        } catch (IllegalPluginAccessException stopped) {
            // Plugin shutdown can race callback scheduling. Never retry the financial operation here.
        }
    }

    private void usage(CommandSender sender) {
        reply(sender, "/tneexamplepool create <account-key> <display name>\n"
                + "/tneexamplepool list [offset] | find <account-key>\n"
                + "/tneexamplepool rename <account-key> <display name>\n"
                + "/tneexamplepool balance <account-uuid> [currency]\n"
                + "/tneexamplepool balances <account-uuid>\n"
                + "/tneexamplepool pay <source-account-uuid> <player-uuid> <currency> <amount> <saved-key>\n"
                + "/tneexamplepool charge <destination-account-uuid> <player-uuid> <currency> <amount> <saved-key>\n"
                + "/tneexamplepool transfer <source-account-uuid> <destination-account-uuid> <currency> <amount> <saved-key>\n"
                + "/tneexamplepool receipt <saved-key> | details <transaction-uuid>\n"
                + "/tneexamplepool history <account-uuid> [offset]\n"
                + "/tneexamplepool post <saved-key> <source-uuid> <destination-uuid> <currency> <amount> [more groups]\n"
                + "/tneexamplepool reverse <transaction-uuid> <saved-key> <reason>");
    }
}
