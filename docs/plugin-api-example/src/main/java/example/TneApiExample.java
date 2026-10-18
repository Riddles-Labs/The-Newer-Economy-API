package example;

import com.cmriddles.thenewereconomy.api.EconomyClient;
import com.cmriddles.thenewereconomy.api.TneApi;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Player balance reads plus separately authorized operator account tools. */
public final class TneApiExample extends JavaPlugin {
    @Override
    public void onEnable() {
        Objects.requireNonNull(getCommand("tneexamplebalance"))
                .setExecutor(this::balance);
        PoolCommands commands = new PoolCommands(this);
        commands.register();
        new CommerceCommands(this, commands).register();
        // TNE initializes storage asynchronously. Check readiness per request.
    }

    private boolean balance(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Run this command as a player.");
            return true;
        }
        if (args.length > 1) {
            player.sendMessage("Usage: /tneexamplebalance [currency]");
            return true;
        }

        TneApi api = getServer().getServicesManager().load(TneApi.class);
        if (api == null || !api.isAvailable()) {
            player.sendMessage("The economy is starting or unavailable. Please try again shortly.");
            return true;
        }

        EconomyClient economy = api.client(getName());
        String currency = args.length == 0 ? economy.defaultCurrency().id() : args[0];
        economy.balance(player.getUniqueId(), currency).whenComplete((result, error) -> {
            if (!isEnabled()) return;
            if (error != null) {
                getLogger().log(Level.WARNING, "TNE balance request failed", error);
            } else if (!result.isSuccess()) {
                getLogger().warning("TNE balance request: " + result.failure().code()
                        + "; reference=" + result.failure().correlationId());
            }
            // Completion may run off-thread. The entity scheduler is safe on Paper and Folia.
            player.getScheduler().run(this, task -> {
                if (error != null) {
                    player.sendMessage("Unable to read your balance. Please try again shortly.");
                } else if (!result.isSuccess()) {
                    player.sendMessage("Unable to read your balance: " + result.failure().code());
                } else {
                    var current = result.value();
                    player.sendMessage("Balance: " + current.amount().toPlainString()
                            + " " + current.currency());
                }
            }, null);
        });
        return true;
    }
}
