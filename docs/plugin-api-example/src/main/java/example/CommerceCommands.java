package example;

import com.cmriddles.thenewereconomy.api.TneApi;
import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.presentation.MenuView;
import com.cmriddles.thenewereconomy.api.presentation.MoneyStyle;
import com.cmriddles.thenewereconomy.api.query.HistoryQuery;
import com.cmriddles.thenewereconomy.api.request.RequestDraft;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Optional operator examples for the same generic economy client. No financial action auto-confirms. */
final class CommerceCommands {
    private final JavaPlugin plugin;
    private final PoolCommands replies;
    CommerceCommands(JavaPlugin plugin, PoolCommands replies) { this.plugin = plugin; this.replies = replies; }
    void register() { Objects.requireNonNull(plugin.getCommand("tneexampleapi")).setExecutor(this::execute); }
    private boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tneexample.api")) { sender.sendMessage("You do not have permission to use the API examples."); return true; }
        if (!(sender instanceof Player) && !(sender instanceof ConsoleCommandSender)) { sender.sendMessage("Use a player or the server console."); return true; }
        TneApi api = plugin.getServer().getServicesManager().load(TneApi.class);
        if (api == null || !api.isAvailable()) { replies.reply(sender, "The economy is starting or unavailable."); return true; }
        if (args.length == 0) { usage(sender); return true; }
        var economy = api.client(plugin.getName());
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "invoice" -> {
                    if (args.length < 6) { usage(sender); break; }
                    String message = args.length == 6 ? "" : String.join(" ", Arrays.copyOfRange(args, 6, args.length));
                    var draft = new RequestDraft(account(args[1]), UUID.fromString(args[2]), args[3], new BigDecimal(args[4]), message,
                            Optional.empty(), args[5]);
                    replies.complete(sender, economy.requests().create(draft), value -> "Pending invoice: " + value.id() + " | No payer funds were debited.");
                }
                case "requests" -> replies.complete(sender, economy.requests().pending(player(sender).getUniqueId(), 20, 0), values -> values.stream()
                        .map(value -> value.id() + " | " + value.amount().toPlainString() + " " + value.currency()).collect(Collectors.joining("\n")));
                case "accept" -> {
                    if (args.length != 3) { usage(sender); break; }
                    // The player issues this explicit confirmation; never substitute a UUID supplied by another user.
                    replies.complete(sender, economy.requests().accept(UUID.fromString(args[1]), player(sender).getUniqueId(), args[2]), value -> "Paid invoice: " + value.transactionId());
                }
                case "deny" -> {
                    if (args.length != 2) { usage(sender); break; }
                    replies.complete(sender, economy.requests().deny(UUID.fromString(args[1]), player(sender).getUniqueId()), value -> "Request: " + value.status());
                }
                case "cancel-invoice" -> {
                    if (args.length != 2) { usage(sender); break; }
                    replies.complete(sender, economy.requests().cancel(UUID.fromString(args[1])), value -> "Request: " + value.status());
                }
                case "quote" -> {
                    if (args.length != 4) { usage(sender); break; }
                    replies.complete(sender, economy.exchange().quote(player(sender).getUniqueId(), args[1], args[2], new BigDecimal(args[3])), value ->
                            "Quote: " + value.id() + " | Debit " + value.sourceAmount().toPlainString() + " " + value.sourceCurrency()
                            + " | Net " + value.netTargetAmount().toPlainString() + " " + value.targetCurrency()
                            + " | Fee " + value.fee().toPlainString() + " | Tax " + value.tax().toPlainString() + " | Expires " + value.expiresAt());
                }
                case "convert" -> {
                    if (args.length != 3) { usage(sender); break; }
                    replies.complete(sender, economy.exchange().execute(player(sender).getUniqueId(), UUID.fromString(args[1]), args[2]), value -> "Converted: " + value.transactionId());
                }
                case "note" -> {
                    if (args.length != 4) { usage(sender); break; }
                    replies.complete(sender, economy.notes().issue(player(sender).getUniqueId(), args[1], new BigDecimal(args[2]), args[3]), value ->
                            "Note: " + value.noteId() + " | " + value.delivery() + " | Transaction: " + value.transactionId().map(UUID::toString).orElse("none"));
                }
                case "redeem" -> {
                    if (args.length != 2) { usage(sender); break; }
                    replies.complete(sender, economy.notes().redeemHeld(player(sender).getUniqueId(), args[1]), value -> "Note: " + value.noteId() + " | " + value.delivery());
                }
                case "inspect-note" -> {
                    if (args.length != 2) { usage(sender); break; }
                    replies.complete(sender, economy.notes().inspect(UUID.fromString(args[1])), value -> value.map(note -> note.id() + " | " + note.status()
                            + " | " + note.amount().toPlainString() + " " + note.currency()).orElse("Note not found"));
                }
                case "batch" -> {
                    if (args.length < 3) { usage(sender); break; }
                    var accounts = Arrays.stream(args, 2, args.length).map(CommerceCommands::account).toList();
                    replies.complete(sender, economy.queries().balances(accounts, List.of(args[1])), values -> values.stream()
                            .map(value -> value.account().name() + " | " + value.amount().toPlainString() + " " + value.currency()).collect(Collectors.joining("\n")));
                }
                case "filter" -> {
                    if (args.length < 5 || args.length > 6) { usage(sender); break; }
                    var query = new HistoryQuery(account(args[1]), Optional.of(args[2]), Optional.empty(), Optional.of(Instant.now().minusSeconds(30L * 86400)),
                            Optional.empty(), Map.of(args[3], args[4]), 10, args.length == 6 ? Integer.parseInt(args[5]) : 0);
                    replies.complete(sender, economy.queries().history(query), values -> values.stream().map(value -> value.transactionId().toString()).collect(Collectors.joining("\n")));
                }
                case "top" -> {
                    if (args.length != 2) { usage(sender); break; }
                    replies.complete(sender, economy.queries().leaderboard(args[1], 10, 0), value -> value.entries().stream()
                            .map(entry -> entry.playerId() + " | " + entry.value().toPlainString()).collect(Collectors.joining("\n")));
                }
                case "format" -> {
                    if (args.length != 4) { usage(sender); break; }
                    replies.complete(sender, economy.presentation().format(args[1], new BigDecimal(args[2]), MoneyStyle.valueOf(args[3].toUpperCase(Locale.ROOT))), value -> value);
                }
                case "menu" -> {
                    if (args.length != 2) { usage(sender); break; }
                    replies.complete(sender, economy.presentation().open(player(sender).getUniqueId(), MenuView.valueOf(args[1].toUpperCase(Locale.ROOT))), value -> "Opened: " + value);
                }
                default -> usage(sender);
            }
        } catch (IllegalArgumentException invalid) { replies.reply(sender, "Invalid input: " + invalid.getMessage()); }
        return true;
    }
    private static AccountRef account(String value) { return AccountRef.account(UUID.fromString(value)); }
    private static Player player(CommandSender sender) {
        if (sender instanceof Player value) return value;
        throw new IllegalArgumentException("This action requires the actual player to run the command.");
    }
    private void usage(CommandSender sender) { replies.reply(sender, "/tneexampleapi invoice <account-uuid> <payer-uuid> <currency> <amount> <saved-key> [message]\n"
            + "requests | accept <request-uuid> <saved-key> | deny <request-uuid> | cancel-invoice <request-uuid>\n"
            + "quote <source> <target> <amount> | convert <quote-uuid> <saved-key>\n"
            + "note <currency> <amount> <saved-key> | redeem <saved-key> | inspect-note <note-uuid>\n"
            + "batch <currency> <account-uuid> [more accounts] | filter <account-uuid> <currency> <metadata-key> <value> [offset]\n"
            + "top <currency|all> | format <currency> <amount> <AMOUNT|NAME|SYMBOL> | menu <WALLET|EXCHANGE|LEADERBOARD|TRANSACTIONS>"); }
}
