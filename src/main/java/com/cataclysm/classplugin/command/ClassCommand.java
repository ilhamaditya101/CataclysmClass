package com.cataclysm.classplugin.command;

import com.cataclysm.classplugin.gui.ClassMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ClassCommand implements CommandExecutor {
    private final ClassMenu menu;
    public ClassCommand(ClassMenu menu) { this.menu = menu; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Command ini hanya untuk player.");
            return true;
        }
        menu.open(player);
        return true;
    }
}
