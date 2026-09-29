package dev.smp.settings;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SettingsPlugin extends JavaPlugin {

    private SettingsManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new SettingsManager(this);

        // Load settings for players already online (e.g. after /reload)
        for (Player p : Bukkit.getOnlinePlayers()) {
            manager.load(p);
            manager.applyAll(p);
        }

        getServer().getPluginManager().registerEvents(new SettingsListener(this), this);

        SettingsCommand cmd = new SettingsCommand(this);
        getCommand("settings").setExecutor(cmd);
        getCommand("settings").setTabCompleter(cmd);

        // Re-apply night vision if it gets removed (milk, commands, etc.)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (manager.get(p, Setting.NIGHT_VISION)
                        && !p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                    manager.apply(p, Setting.NIGHT_VISION);
                }
            }
        }, 100L, 100L);
    }

    public SettingsManager manager() {
        return manager;
    }
}
