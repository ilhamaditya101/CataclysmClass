package com.cataclysm.classplugin.gui;

import com.cataclysm.classplugin.CataclysmClassPlugin;
import com.cataclysm.classplugin.manager.ClassManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ClassMenu {
    private final CataclysmClassPlugin plugin;
    private final ClassManager classManager;

    public ClassMenu(CataclysmClassPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
    }

    public void open(Player player) {
        open(player, false);
    }

    public void open(Player player, boolean editMode) {
        String title = color(plugin.getConfig().getString("gui.title", "&8Cataclysm Class"));
        int size = plugin.getConfig().getInt("gui.size", 27);
        Inventory inv = plugin.getServer().createInventory(null, size, title);

        loadEditableItems(inv);

        ConfigurationSection classes = plugin.getConfig().getConfigurationSection("classes");
        if (classes != null) {
            for (String id : classes.getKeys(false)) {
                String path = "classes." + id;
                int slot = plugin.getConfig().getInt(path + ".slot", -1);
                if (slot < 0 || slot >= size) continue;
                inv.setItem(slot, createClassItem(id));
            }
        }
        player.openInventory(inv);
    }

    private void loadEditableItems(Inventory inv) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("gui.editor-items");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                if (slot >= 0 && slot < inv.getSize()) {
                    ItemStack item = section.getItemStack(key);
                    if (item != null) inv.setItem(slot, item);
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    public ItemStack createClassItem(String id) {
        String path = "classes." + id;
        Material material = Material.matchMaterial(plugin.getConfig().getString(path + ".material", "STONE"));
        if (material == null) material = Material.STONE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(color(plugin.getConfig().getString(path + ".display-name", id)));
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList(path + ".lore")) lore.add(color(line));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isClassSlot(int slot) {
        ConfigurationSection classes = plugin.getConfig().getConfigurationSection("classes");
        if (classes == null) return false;
        for (String id : classes.getKeys(false)) {
            if (plugin.getConfig().getInt("classes." + id + ".slot", -1) == slot) return true;
        }
        return false;
    }

    public void saveEditorItems(Inventory inventory) {
        ConfigurationSection classes = plugin.getConfig().getConfigurationSection("classes");
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            if (isClassSlot(slot)) continue;
            ItemStack item = inventory.getItem(slot);
            plugin.getConfig().set("gui.editor-items." + slot, item == null || item.getType() == Material.AIR ? null : item);
        }
        plugin.saveConfig();
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
