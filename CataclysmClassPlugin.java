package com.cataclysm.classplugin;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.event.skill.XpGainEvent;
import dev.aurelium.auraskills.api.skill.Skill;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class CataclysmClassPlugin extends JavaPlugin implements Listener {
    private static final String MENU_PREFIX = "CataclysmClass:";
    private final Map<UUID, String> classes = new HashMap<>();
    private AuraSkillsApi auraSkills;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadData();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("class")).setExecutor(new ClassCommand());
        Objects.requireNonNull(getCommand("classadmin")).setExecutor(new AdminCommand());

        if (Bukkit.getPluginManager().getPlugin("AuraSkills") == null) {
            getLogger().severe("AuraSkills was not found. Disabling CataclysmClass.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        try {
            auraSkills = AuraSkillsApi.get();
            getLogger().info("CataclysmClass enabled with AuraSkills integration.");
        } catch (Throwable t) {
            getLogger().severe("Could not access AuraSkills API: " + t.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        saveData();
    }

    private void loadData() {
        getDataFolder().mkdirs();
        saveResource("data.yml", false);
        var cfg = new org.bukkit.configuration.file.YamlConfiguration();
        try {
            cfg.load(new java.io.File(getDataFolder(), "data.yml"));
            ConfigurationSection section = cfg.getConfigurationSection("players");
            if (section != null) {
                for (String uuid : section.getKeys(false)) {
                    String clazz = section.getString(uuid + ".class");
                    if (clazz != null) classes.put(UUID.fromString(uuid), clazz.toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception e) {
            getLogger().warning("Could not load data.yml: " + e.getMessage());
        }
    }

    private void saveData() {
        var cfg = new org.bukkit.configuration.file.YamlConfiguration();
        for (Map.Entry<UUID, String> e : classes.entrySet()) {
            cfg.set("players." + e.getKey() + ".class", e.getValue());
        }
        try {
            cfg.save(new java.io.File(getDataFolder(), "data.yml"));
        } catch (Exception e) {
            getLogger().warning("Could not save data.yml: " + e.getMessage());
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String msg(String key, Map<String, String> replacements) {
        String s = getConfig().getString("messages." + key, "");
        for (Map.Entry<String, String> e : replacements.entrySet()) s = s.replace("{" + e.getKey() + "}", e.getValue());
        return color(s);
    }

    private Material material(String path, Material fallback) {
        Material m = Material.matchMaterial(getConfig().getString(path, fallback.name()));
        return m == null ? fallback : m;
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> out = new ArrayList<>();
            for (String line : lore) out.add(color(line));
            meta.setLore(out);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void openClassMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, color(MENU_PREFIX + getConfig().getString("gui.title", "&8Choose Your Class")));
        ItemStack white = item(material("gui.filler", Material.WHITE_STAINED_GLASS_PANE), " ", Collections.emptyList());
        ItemStack black = item(material("gui.bottom", Material.BLACK_STAINED_GLASS_PANE), " ", Collections.emptyList());
        for (int i = 0; i < 27; i++) inv.setItem(i, white);
        for (int i = 27; i < 36; i++) inv.setItem(i, black);

        inv.setItem(10, classItem("fighter", material("gui.fighter", Material.IRON_SWORD)));
        inv.setItem(12, classItem("ranger", material("gui.ranger", Material.BOW)));
        inv.setItem(14, classItem("tanker", material("gui.tanker", Material.SHIELD)));
        inv.setItem(16, classItem("mage", material("gui.mage", Material.NETHER_STAR)));
        inv.setItem(31, item(material("gui.exit", Material.BARRIER), "&cExit", Collections.singletonList("&7Click to close")));
        player.openInventory(inv);
    }

    private ItemStack classItem(String id, Material material) {
        String name = getConfig().getString("classes." + id + ".display-name", id);
        List<String> lore = new ArrayList<>();
        lore.add("");
        ConfigurationSection multipliers = getConfig().getConfigurationSection("classes." + id + ".multipliers");
        if (multipliers != null) {
            for (String skill : multipliers.getKeys(false)) {
                lore.add("&7" + pretty(skill) + " &f" + multipliers.getDouble(skill, 1.0) + "x");
            }
        }
        lore.add("");
        lore.add("&eClick to choose this class");
        return item(material, name, lore);
    }

    private String pretty(String skill) {
        String[] p = skill.replace('_', ' ').split(" ");
        StringBuilder b = new StringBuilder();
        for (String s : p) {
            if (s.isEmpty()) continue;
            b.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).append(' ');
        }
        return b.toString().trim();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onXp(XpGainEvent event) {
        String clazz = classes.get(event.getPlayer().getUniqueId());
        if (clazz == null) return;
        ConfigurationSection multipliers = getConfig().getConfigurationSection("classes." + clazz + ".multipliers");
        if (multipliers == null) return;
        Skill skill = event.getSkill();
        if (skill == null || skill.getId() == null) return;
        String key = skill.getId().getKey().toLowerCase(Locale.ROOT);
        if (!multipliers.contains(key)) return;
        double multiplier = multipliers.getDouble(key, 1.0);
        if (multiplier <= 0) return;
        event.setAmount(event.getAmount() * multiplier);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith(color(MENU_PREFIX))) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (event.getRawSlot() == 31) { player.closeInventory(); return; }
        String clazz = switch (event.getRawSlot()) {
            case 10 -> "fighter";
            case 12 -> "ranger";
            case 14 -> "tanker";
            case 16 -> "mage";
            default -> null;
        };
        if (clazz == null) return;
        if (classes.containsKey(player.getUniqueId())) {
            player.sendMessage(msg("already-selected", Map.of("class", displayClass(classes.get(player.getUniqueId())))));
            player.closeInventory();
            return;
        }
        classes.put(player.getUniqueId(), clazz);
        saveData();
        player.sendMessage(msg("selected", Map.of("class", displayClass(clazz))));
        player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().startsWith(color(MENU_PREFIX))) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // Intentionally empty: class selection is one-time only.
    }

    private String displayClass(String id) {
        return color(getConfig().getString("classes." + id + ".display-name", id));
    }

    private class ClassCommand implements CommandExecutor {
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) { sender.sendMessage(msg("player-only", Map.of())); return true; }
            if (classes.containsKey(player.getUniqueId())) {
                player.sendMessage(msg("already-selected", Map.of("class", displayClass(classes.get(player.getUniqueId())))));
                return true;
            }
            openClassMenu(player);
            return true;
        }
    }

    private class AdminCommand implements CommandExecutor {
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("cataclysmclass.admin")) { sender.sendMessage(msg("no-permission", Map.of())); return true; }
            if (args.length == 0) { sender.sendMessage(msg("usage", Map.of())); return true; }
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("reload")) {
                reloadConfig();
                sender.sendMessage(msg("reloaded", Map.of()));
                return true;
            }
            if ((sub.equals("reset") || sub.equals("info")) && args.length < 2) { sender.sendMessage(msg("usage", Map.of())); return true; }
            if (sub.equals("reset")) {
                Player online = Bukkit.getPlayerExact(args[1]);
                UUID uuid = online != null ? online.getUniqueId() : findUuid(args[1]);
                if (uuid == null) { sender.sendMessage(msg("player-not-found", Map.of())); return true; }
                classes.remove(uuid); saveData();
                sender.sendMessage(msg("reset", Map.of("player", args[1])));
                return true;
            }
            if (sub.equals("info")) {
                Player online = Bukkit.getPlayerExact(args[1]);
                UUID uuid = online != null ? online.getUniqueId() : findUuid(args[1]);
                if (uuid == null) { sender.sendMessage(msg("player-not-found", Map.of())); return true; }
                String clazz = classes.get(uuid);
                if (clazz == null) { sender.sendMessage(msg("no-class", Map.of())); return true; }
                sender.sendMessage(msg("info", Map.of("player", args[1], "class", displayClass(clazz))));
                return true;
            }
            sender.sendMessage(msg("usage", Map.of())); return true;
        }
    }

    private UUID findUuid(String name) {
        for (UUID uuid : classes.keySet()) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
            if (p.getName() != null && p.getName().equalsIgnoreCase(name)) return uuid;
        }
        return null;
    }
}
