package com.cataclysm.classplugin.gui;

import com.cataclysm.classplugin.CataclysmClassPlugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClassMenu {

    private final CataclysmClassPlugin plugin;

    public ClassMenu(CataclysmClassPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        String title = color(
                plugin.getConfig().getString(
                        "gui.title",
                        "&8Cataclysm Class"
                )
        );

        int size = plugin.getConfig()
                .getInt("gui.size", 27);

        Inventory inventory = plugin.getServer()
                .createInventory(null, size, title);

        var section = plugin.getConfig()
                .getConfigurationSection("classes");

        if (section == null) {
            player.openInventory(inventory);
            return;
        }

        for (String id : section.getKeys(false)) {

            String path = "classes." + id;

            String materialName = plugin.getConfig()
                    .getString(
                            path + ".material",
                            "STONE"
                    );

            Material material =
                    Material.matchMaterial(materialName);

            if (material == null) {
                material = Material.STONE;
            }

            ItemStack item = new ItemStack(material);

            ItemMeta meta = item.getItemMeta();

            if (meta == null) {
                continue;
            }

            String displayName = plugin.getConfig()
                    .getString(
                            path + ".display-name",
                            id
                    );

            meta.setDisplayName(color(displayName));

            List<String> lore =
                    plugin.getConfig()
                            .getStringList(path + ".lore");

            List<String> coloredLore = new ArrayList<>();

            for (String line : lore) {
                coloredLore.add(color(line));
            }

            meta.setLore(coloredLore);

            item.setItemMeta(meta);

            int slot = plugin.getConfig()
                    .getInt(path + ".slot", 0);

            inventory.setItem(slot, item);
        }

        player.openInventory(inventory);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}
