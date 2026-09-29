package dev.smp.settings;

import org.bukkit.NamespacedKey;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stores settings in each player's PersistentDataContainer (saved automatically with player data). */
public class SettingsManager {

    private final SettingsPlugin plugin;
    private final Map<Setting, NamespacedKey> keys = new EnumMap<>(Setting.class);
    private final Map<UUID, boolean[]> cache = new ConcurrentHashMap<>();

    public SettingsManager(SettingsPlugin plugin) {
        this.plugin = plugin;
        for (Setting s : Setting.values()) {
            keys.put(s, new NamespacedKey(plugin, "setting_" + s.id()));
        }
    }

    public boolean isAvailable(Setting s) {
        return plugin.getConfig().getBoolean("settings." + s.id() + ".enabled", true);
    }

    public boolean defaultValue(Setting s) {
        return plugin.getConfig().getBoolean("settings." + s.id() + ".default", s.defaultValue());
    }

    /** Loads a player's settings into the cache (call on join). */
    public void load(Player p) {
        boolean[] arr = new boolean[Setting.values().length];
        for (Setting s : Setting.values()) arr[s.ordinal()] = readPdc(p, s);
        cache.put(p.getUniqueId(), arr);
    }

    public void unload(Player p) {
        cache.remove(p.getUniqueId());
    }

    public boolean get(Player p, Setting s) {
        if (!isAvailable(s)) return defaultValue(s);
        boolean[] arr = cache.get(p.getUniqueId());
        if (arr != null) return arr[s.ordinal()];
        return readPdc(p, s);
    }

    public void set(Player p, Setting s, boolean value) {
        p.getPersistentDataContainer().set(keys.get(s), PersistentDataType.BYTE, (byte) (value ? 1 : 0));
        boolean[] arr = cache.get(p.getUniqueId());
        if (arr != null) arr[s.ordinal()] = value;
        apply(p, s);
    }

    private boolean readPdc(Player p, Setting s) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        Byte b = pdc.get(keys.get(s), PersistentDataType.BYTE);
        return b == null ? defaultValue(s) : b == 1;
    }

    public void applyAll(Player p) {
        for (Setting s : Setting.values()) apply(p, s);
    }

    /** Applies settings that need an active effect on the player. */
    public void apply(Player p, Setting s) {
        switch (s) {
            case NIGHT_VISION -> {
                if (get(p, s)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,
                            PotionEffect.INFINITE_DURATION, 0, false, false, false));
                } else {
                    PotionEffect e = p.getPotionEffect(PotionEffectType.NIGHT_VISION);
                    if (e != null && e.isInfinite()) p.removePotionEffect(PotionEffectType.NIGHT_VISION);
                }
            }
            case CLEAR_WEATHER -> {
                if (get(p, s)) p.setPlayerWeather(WeatherType.CLEAR);
                else p.resetPlayerWeather();
            }
            case ALWAYS_DAY -> {
                if (get(p, s)) p.setPlayerTime(6000L, false);
                else p.resetPlayerTime();
            }
            default -> { }
        }
    }
}
