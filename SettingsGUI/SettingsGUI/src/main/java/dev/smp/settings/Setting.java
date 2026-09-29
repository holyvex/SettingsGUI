package dev.smp.settings;

import org.bukkit.Material;

import java.util.List;

public enum Setting {
    MOB_SPAWNING("Mob Spawning", Material.ZOMBIE_HEAD, true,
            "Turn this off to stop mobs from", "naturally spawning around you."),
    PRIVATE_MESSAGES("Private Messages", Material.WRITABLE_BOOK, true,
            "Allow other players to send", "you private messages."),
    PUBLIC_CHAT("Public Chat", Material.PAPER, true,
            "Show or hide chat messages", "from other players."),
    DEATH_MESSAGES("Death Messages", Material.SKELETON_SKULL, true,
            "Show or hide player death", "messages."),
    JOIN_LEAVE_MESSAGES("Join / Leave Messages", Material.OAK_DOOR, true,
            "Show or hide join and leave", "messages."),
    ADVANCEMENT_MESSAGES("Advancement Messages", Material.EXPERIENCE_BOTTLE, true,
            "Show or hide advancement", "messages in chat."),
    ITEM_PICKUP("Item Pickup", Material.HOPPER, true,
            "Turn this off to stop picking", "up items from the ground."),
    NIGHT_VISION("Night Vision", Material.GOLDEN_CARROT, false,
            "Permanent night vision", "without particles."),
    CLEAR_WEATHER("Clear Weather", Material.WATER_BUCKET, false,
            "Only you see clear skies,", "no rain or thunder."),
    ALWAYS_DAY("Always Day", Material.CLOCK, false,
            "Only you see it as", "permanent daytime.");

    private final String display;
    private final Material icon;
    private final boolean defaultValue;
    private final List<String> description;

    Setting(String display, Material icon, boolean defaultValue, String... description) {
        this.display = display;
        this.icon = icon;
        this.defaultValue = defaultValue;
        this.description = List.of(description);
    }

    public String display() { return display; }
    public Material icon() { return icon; }
    public boolean defaultValue() { return defaultValue; }
    public List<String> description() { return description; }
    public String id() { return name().toLowerCase(); }
}
