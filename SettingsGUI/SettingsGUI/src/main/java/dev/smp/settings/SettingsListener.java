package dev.smp.settings;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public class SettingsListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Set<String> MSG_COMMANDS =
            Set.of("msg", "tell", "w", "whisper", "m", "t", "pm");

    private final SettingsPlugin plugin;
    private final SettingsManager manager;

    public SettingsListener(SettingsPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.manager();
    }

    // ---------------------------------------------------------------- GUI

    @EventHandler
    public void onGuiClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof SettingsGui gui)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        Setting s = gui.settingAt(e.getSlot());
        if (s == null) return;

        boolean now = !manager.get(p, s);
        manager.set(p, s, now);
        gui.render();
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, now ? 1.2f : 0.8f);
    }

    @EventHandler
    public void onGuiDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof SettingsGui) e.setCancelled(true);
    }

    // ------------------------------------------------------ join / quit

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        manager.load(p);
        manager.applyAll(p);
        if (manager.isAvailable(Setting.JOIN_LEAVE_MESSAGES)) {
            Component msg = e.joinMessage();
            e.joinMessage(null);
            broadcast(msg, Setting.JOIN_LEAVE_MESSAGES);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent e) {
        if (manager.isAvailable(Setting.JOIN_LEAVE_MESSAGES)) {
            Component msg = e.quitMessage();
            e.quitMessage(null);
            broadcast(msg, Setting.JOIN_LEAVE_MESSAGES);
        }
        manager.unload(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> manager.applyAll(p));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        manager.applyAll(e.getPlayer());
    }

    // --------------------------------------------------------- messages

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        if (!manager.isAvailable(Setting.DEATH_MESSAGES)) return;
        Component msg = e.deathMessage();
        e.deathMessage(null);
        broadcast(msg, Setting.DEATH_MESSAGES);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        if (!manager.isAvailable(Setting.ADVANCEMENT_MESSAGES)) return;
        Component msg = e.message();
        e.message(null);
        broadcast(msg, Setting.ADVANCEMENT_MESSAGES);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (!manager.isAvailable(Setting.PUBLIC_CHAT)) return;
        Player sender = e.getPlayer();
        e.viewers().removeIf(a -> a instanceof Player p
                && !p.equals(sender)
                && !manager.get(p, Setting.PUBLIC_CHAT));
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!manager.isAvailable(Setting.PRIVATE_MESSAGES)) return;
        String raw = e.getMessage();
        if (raw.length() < 2) return;
        String[] parts = raw.substring(1).split("\\s+");
        if (parts.length < 2) return;

        String label = parts[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);
        if (!MSG_COMMANDS.contains(label)) return;

        Player target = Bukkit.getPlayerExact(parts[1]);
        if (target == null || target.equals(e.getPlayer())) return;
        if (!manager.get(target, Setting.PRIVATE_MESSAGES)
                && !e.getPlayer().hasPermission("settings.bypass.msg")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(MM.deserialize("<red>That player has private messages disabled."));
        }
    }

    private void broadcast(Component msg, Setting setting) {
        if (msg == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (manager.get(p, setting)) p.sendMessage(msg);
        }
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    // ---------------------------------------------------- mob spawning

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!manager.isAvailable(Setting.MOB_SPAWNING)) return;

        SpawnReason reason = e.getSpawnReason();
        boolean blocked = reason == SpawnReason.NATURAL
                || reason == SpawnReason.PATROL
                || reason == SpawnReason.VILLAGE_INVASION
                || (reason == SpawnReason.SPAWNER
                    && plugin.getConfig().getBoolean("mob-spawning.include-spawners", false));
        if (!blocked) return;

        if (plugin.getConfig().getBoolean("mob-spawning.hostile-only", false)
                && !(e.getEntity() instanceof Enemy)) return;

        double radius = plugin.getConfig().getDouble("mob-spawning.radius", 64);
        boolean anyPlayer = false;
        for (Player p : e.getLocation().getNearbyPlayers(radius)) {
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            anyPlayer = true;
            if (manager.get(p, Setting.MOB_SPAWNING)) return; // someone nearby still wants mobs
        }
        if (anyPlayer) e.setCancelled(true);
    }

    // ------------------------------------------------------ item pickup

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!manager.isAvailable(Setting.ITEM_PICKUP)) return;
        if (!manager.get(p, Setting.ITEM_PICKUP)) e.setCancelled(true);
    }
}
