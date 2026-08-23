package com.cataclysm.classplugin.listener;

import com.cataclysm.classplugin.CataclysmClassPlugin;
import com.cataclysm.classplugin.manager.ClassManager;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ClassMenuListener implements Listener {

    private final CataclysmClassPlugin plugin;
    private final ClassManager classManager;

    public ClassMenuListener(
            CataclysmClassPlugin plugin,
            ClassManager classManager
    ) {
        this.plugin = plugin;
        this.classManager = classManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = ChatColor.translateAlternateColorCodes(
                '&',
                plugin.getConfig().getString(
                        "gui.title",
                        "&8Cataclysm Class"
                )
        );

        if (!event.getView().getTitle().equals(title)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() < 0 ||
                event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        var section = plugin.getConfig()
                .getConfigurationSection("classes");

        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {

            String path = "classes." + id;

            int slot = plugin.getConfig()
                    .getInt(path + ".slot", -1);

            if (event.getRawSlot() != slot) {
                continue;
            }

            String displayName = plugin.getConfig()
                    .getString(
                            path + ".display-name",
                            id
                    );

            classManager.setClass(player, id);

            String message = plugin.getConfig()
                    .getString(
                            "messages.selected",
                            "&cCataclysmClass &fBerhasil memilih class &e{class}&f!"
                    );

            message = message.replace(
                    "{class}",
                    ChatColor.stripColor(
                            ChatColor.translateAlternateColorCodes(
                                    '&',
                                    displayName
                            )
                    )
            );

            player.sendMessage(
                    ChatColor.translateAlternateColorCodes(
                            '&',
                            message
                    )
            );

            player.closeInventory();

            return;
        }
    }
}
