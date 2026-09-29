package dev.smp.settings;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class SettingsCommand implements CommandExecutor, TabCompleter {

    private final SettingsPlugin plugin;

    public SettingsCommand(SettingsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("settings.admin")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>No permission."));
                return true;
            }
            plugin.reloadConfig();
            plugin.getServer().getOnlinePlayers().forEach(p -> plugin.manager().applyAll(p));
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>SettingsGUI config reloaded."));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can open the settings menu.");
            return true;
        }
        new SettingsGui(plugin, player).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("settings.admin")) return List.of("reload");
        return List.of();
    }
}
