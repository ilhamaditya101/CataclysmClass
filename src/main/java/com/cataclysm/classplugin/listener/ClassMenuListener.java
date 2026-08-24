package com.cataclysm.classplugin.listener;

import com.cataclysm.classplugin.CataclysmClassPlugin;
import com.cataclysm.classplugin.gui.ClassMenu;
import com.cataclysm.classplugin.manager.ClassManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public final class ClassMenuListener implements Listener {
    private final CataclysmClassPlugin plugin;
    private final ClassManager manager;
    private final ClassMenu menu;

    public ClassMenuListener(CataclysmClassPlugin plugin, ClassManager manager, ClassMenu menu) {
        this.plugin = plugin; this.manager = manager; this.menu = menu;
    }

    private boolean isMenu(InventoryClickEvent e) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.title", "&8Cataclysm Class"));
        return e.getView().getTitle().equals(title);
    }

    private boolean isMenu(InventoryDragEvent e) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.title", "&8Cataclysm Class"));
        return e.getView().getTitle().equals(title);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        manager.handlePendingReset(e.getPlayer());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!isMenu(e)) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;

        int topSize = e.getView().getTopInventory().getSize();
        int slot = e.getRawSlot();
        boolean edit = player.hasPermission("cataclysmclass.admin") && plugin.getConfig().getBoolean("gui.edit-mode-enabled", true)
                && player.getOpenInventory().getTitle().equals(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.title", "&8Cataclysm Class")))
                && EditState.isEditing(player);

        if (edit) {
            if (slot >= 0 && slot < topSize && menu.isClassSlot(slot)) {
                e.setCancelled(true);
            }
            // All non-class slots behave like a normal inventory in edit mode.
            return;
        }

        e.setCancelled(true);
        if (slot < 0 || slot >= topSize) return;

        String selected = manager.getClass(player);
        if (selected != null) {
            player.sendMessage(color("&cKamu sudah memilih class &e" + selected + "&c."));
            player.closeInventory();
            return;
        }

        for (String id : plugin.getConfig().getConfigurationSection("classes").getKeys(false)) {
            int classSlot = plugin.getConfig().getInt("classes." + id + ".slot", -1);
            if (classSlot != slot) continue;
            if (!manager.setClass(player, id)) {
                player.sendMessage(ChatColor.RED + "Class gagal dipilih. Coba lagi.");
                return;
            }
            String msg = plugin.getConfig().getString("messages.selected", "&cCataclysmClass &fBerhasil memilih class &e{class}&f!")
                    .replace("{class}", ChatColor.stripColor(color(plugin.getConfig().getString("classes." + id + ".display-name", id))));
            player.sendMessage(color(msg));
            player.closeInventory();
            return;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!isMenu(e)) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!EditState.isEditing(player)) {
            e.setCancelled(true);
            return;
        }
        for (int slot : e.getRawSlots()) {
            if (slot < e.getView().getTopInventory().getSize() && menu.isClassSlot(slot)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.title", "&8Cataclysm Class"));
        if (!e.getView().getTitle().equals(title)) return;
        if (EditState.isEditing(player)) {
            menu.saveEditorItems(e.getInventory());
            EditState.setEditing(player, false);
            player.sendMessage(color("&aCataclysmClass &fGUI berhasil disimpan."));
        }
    }

    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }

    public static final class EditState {
        private static final java.util.Set<java.util.UUID> EDITORS = java.util.concurrent.ConcurrentHashMap.newKeySet();
        public static boolean isEditing(Player p) { return EDITORS.contains(p.getUniqueId()); }
        public static void setEditing(Player p, boolean value) {
            if (value) EDITORS.add(p.getUniqueId()); else EDITORS.remove(p.getUniqueId());
        }
    }
}
