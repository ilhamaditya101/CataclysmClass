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
import org.bukkit.configuration.file.FileConfiguration;

public final class CataclysmClassPlugin extends JavaPlugin {
    private LuckPerms luckPerms;
    private ClassManager classManager;
    private ClassMenu classMenu;
    private FileConfiguration data;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("data.yml", false);
        data = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.File(getDataFolder(), "data.yml"));

        RegisteredServiceProvider<LuckPerms> registration =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (registration == null) {
            getLogger().severe("LuckPerms tidak ditemukan. Plugin dinonaktifkan.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        luckPerms = registration.getProvider();

        classManager = new ClassManager(this, luckPerms);
        classMenu = new ClassMenu(this, classManager);

        if (getCommand("class") != null) {
            getCommand("class").setExecutor(new ClassCommand(classMenu));
        }
        if (getCommand("cclass") != null) {
            getCommand("cclass").setExecutor(new CCYCommand(this, classMenu, classManager));
        }

        Bukkit.getPluginManager().registerEvents(new ClassMenuListener(this, classManager, classMenu), this);
        getLogger().info("CataclysmClass 1.0.1 enabled.");
    }

    public FileConfiguration getData() { return data; }

    public void saveData() {
        try { data.save(new java.io.File(getDataFolder(), "data.yml")); }
        catch (java.io.IOException e) { getLogger().severe("Gagal menyimpan data.yml: " + e.getMessage()); }
    }

    public LuckPerms getLuckPerms() { return luckPerms; }
    public ClassManager getClassManager() { return classManager; }
    public ClassMenu getClassMenu() { return classMenu; }
}
