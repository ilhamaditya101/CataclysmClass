package com.cataclysm.classplugin.command;

import com.cataclysm.classplugin.CataclysmClassPlugin;
import com.cataclysm.classplugin.gui.ClassMenu;
import com.cataclysm.classplugin.manager.ClassManager;
import com.cataclysm.classplugin.listener.ClassMenuListener.EditState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class CCYCommand implements CommandExecutor {
    private final CataclysmClassPlugin plugin;
    private final ClassMenu menu;
    private final ClassManager manager;

    public CCYCommand(CataclysmClassPlugin plugin, ClassMenu menu, ClassManager manager) {
        this.plugin = plugin; this.menu = menu; this.manager = manager;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cataclysmclass.admin")) {
            sender.sendMessage(ChatColor.RED + "Tidak punya permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/cclass reload");
            sender.sendMessage(ChatColor.YELLOW + "/cclass reset <player>");
            sender.sendMessage(ChatColor.YELLOW + "/cclass edit");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(color(plugin.getConfig().getString("messages.reloaded", "&cCataclysmClass &fConfig berhasil di-reload.")));
            }
            case "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /cclass reset <player>");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(ChatColor.RED + "Player tidak ditemukan.");
                    return true;
                }
                if (!manager.resetClass(target)) {
                    sender.sendMessage(ChatColor.RED + "Player tersebut belum memiliki class.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Class " + target.getName() + " berhasil di-reset.");
            }
            case "edit" -> {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage(ChatColor.RED + "Command ini hanya untuk player.");
                    return true;
                }
                EditState.setEditing(player, true);
                menu.open(player, true);
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "/cclass reset <player> | /cclass edit | /cclass reload");
        }
        return true;
    }

    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }
}
