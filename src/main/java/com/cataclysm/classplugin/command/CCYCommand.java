package com.cataclysm.classplugin.command;

import com.cataclysm.classplugin.CataclysmClassPlugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CCYCommand implements CommandExecutor {

    private final CataclysmClassPlugin plugin;

    public CCYCommand(CataclysmClassPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!sender.hasPermission("cataclysmclass.admin")) {
            sender.sendMessage(
                    ChatColor.RED + "No permission."
            );
            return true;
        }

        if (args.length == 1 &&
                args[0].equalsIgnoreCase("reload")) {

            plugin.reloadConfig();

            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes(
                            '&',
                            plugin.getConfig().getString(
                                    "messages.reloaded",
                                    "&cCataclysmClass &fConfig berhasil di-reload."
                            )
                    )
            );

            return true;
        }

        sender.sendMessage(
                ChatColor.YELLOW + "/cclass reload"
        );

        return true;
    }
}
