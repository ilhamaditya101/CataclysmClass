package com.cataclysm.classplugin;

import com.cataclysm.classplugin.command.CCYCommand;
import com.cataclysm.classplugin.command.ClassCommand;
import com.cataclysm.classplugin.gui.ClassMenu;
import com.cataclysm.classplugin.listener.ClassMenuListener;
import com.cataclysm.classplugin.manager.ClassManager;

import net.luckperms.api.LuckPerms;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class CataclysmClassPlugin extends JavaPlugin {

    private LuckPerms luckPerms;
    private ClassManager classManager;
    private ClassMenu classMenu;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        RegisteredServiceProvider<LuckPerms> provider =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);

        if (provider == null) {
            getLogger().severe("LuckPerms tidak ditemukan!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        luckPerms = provider.getProvider();

        classManager = new ClassManager(this, luckPerms);
        classMenu = new ClassMenu(this);

        getCommand("class").setExecutor(
                new ClassCommand(classMenu)
        );

        getCommand("ccy").setExecutor(
                new CCYCommand(this)
        );

        Bukkit.getPluginManager().registerEvents(
                new ClassMenuListener(
                        this,
                        classManager
                ),
                this
        );

        getLogger().info("CataclysmClass enabled!");
    }

    public ClassManager getClassManager() {
        return classManager;
    }

    public ClassMenu getClassMenu() {
        return classMenu;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
