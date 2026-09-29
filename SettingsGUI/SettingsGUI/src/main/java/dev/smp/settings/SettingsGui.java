package dev.smp.settings;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SettingsGui implements InventoryHolder {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int PER_ROW = 7;

    private final SettingsPlugin plugin;
    private final Player player;
    private final List<Setting> shown = new ArrayList<>();
    private final Inventory inventory;

    public SettingsGui(SettingsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        for (Setting s : Setting.values()) {
            if (plugin.manager().isAvailable(s) && shown.size() < PER_ROW * 2) shown.add(s);
        }
        int groups = Math.max(1, (shown.size() + PER_ROW - 1) / PER_ROW);
        int rows = groups * 2 + 2;
        String title = plugin.getConfig().getString("gui-title", "<dark_gray>Settings");
        this.inventory = Bukkit.createInventory(this, rows * 9, MM.deserialize(title));
        render();
    }

    private int iconSlot(int index) {
        return 10 + (index / PER_ROW) * 18 + (index % PER_ROW);
    }

    /** Returns the setting for a clicked slot (icon or the dye beneath it), or null. */
    public Setting settingAt(int slot) {
        for (int i = 0; i < shown.size(); i++) {
            int icon = iconSlot(i);
            if (slot == icon || slot == icon + 9) return shown.get(i);
        }
        return null;
    }

    public void render() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Component.text(" "));
        fm.setHideTooltip(true);
        filler.setItemMeta(fm);
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        for (int i = 0; i < shown.size(); i++) {
            Setting s = shown.get(i);
            boolean on = plugin.manager().get(player, s);
            int slot = iconSlot(i);

            List<String> lore = new ArrayList<>();
            for (String line : s.description()) lore.add("<gray>" + line);
            lore.add("");
            lore.add("<gray>Status: " + (on ? "<green>Enabled" : "<red>Disabled"));
            lore.add("");
            lore.add("<yellow>Click to toggle");

            inventory.setItem(slot, item(s.icon(), "<yellow><bold>" + s.display(), lore));
            inventory.setItem(slot + 9, item(on ? Material.LIME_DYE : Material.GRAY_DYE,
                    on ? "<green>Enabled" : "<red>Disabled", List.of("<yellow>Click to toggle")));
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack it = new ItemStack(material);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(MM.deserialize("<!italic>" + name));
        meta.lore(lore.stream().map(l -> MM.deserialize("<!italic>" + l)).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        it.setItemMeta(meta);
        return it;
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
