package com.cataclysm.classplugin;

import com.cataclysm.classplugin.command.CCYCommand;
import com.cataclysm.classplugin.command.ClassCommand;
import com.cataclysm.classplugin.gui.ClassMenu;
import com.cataclysm.classplugin.listener.ClassMenuListener;
import com.cataclysm.classplugin.manager.ClassManager;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class CataclysmClassPlugin extends JavaPlugin {

    private LuckPerms luckPerms;
    private AuraSkillsApi auraSkills;
    private ClassManager classManager;
    private ClassMenu classMenu;
    private FileConfiguration data;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        saveResource("data.yml", false);

        data = YamlConfiguration.loadConfiguration(
                new File(
                        getDataFolder(),
                        "data.yml"
                )
        );

        // =====================================================
        // LUCKPERMS
        // =====================================================

        RegisteredServiceProvider<LuckPerms> registration =
                Bukkit.getServicesManager()
                        .getRegistration(LuckPerms.class);

        if (registration == null) {

            getLogger().severe(
                    "LuckPerms tidak ditemukan. Plugin dinonaktifkan."
            );

            Bukkit.getPluginManager()
                    .disablePlugin(this);

            return;
        }

        luckPerms = registration.getProvider();

        // =====================================================
        // AURASKILLS
        // =====================================================

        auraSkills = AuraSkillsApi.get();

        if (auraSkills == null) {

            getLogger().severe(
                    "AuraSkills tidak ditemukan. Plugin dinonaktifkan."
            );

            Bukkit.getPluginManager()
                    .disablePlugin(this);

            return;
        }

        // =====================================================
        // MANAGERS
        // =====================================================

        classManager = new ClassManager(
                this,
                luckPerms,
                auraSkills
        );

        classMenu = new ClassMenu(
                this,
                classManager
        );

        // =====================================================
        // COMMANDS
        // =====================================================

        if (getCommand("class") != null) {

            getCommand("class")
                    .setExecutor(
                            new ClassCommand(classMenu)
                    );
        }

        if (getCommand("cclass") != null) {

            getCommand("cclass")
                    .setExecutor(
                            new CCYCommand(
                                    this,
                                    classMenu,
                                    classManager
                            )
                    );
        }

        // =====================================================
        // LISTENERS
        // =====================================================

        Bukkit.getPluginManager().registerEvents(
                new ClassMenuListener(
                        this,
                        classManager,
                        classMenu
                ),
                this
        );

        getLogger().info(
                "CataclysmClass 1.0.1 enabled."
        );
    }

    // =========================================================
    // DATA
    // =========================================================

    public FileConfiguration getData() {
        return data;
    }

    public void saveData() {

        try {

            data.save(
                    new File(
                            getDataFolder(),
                            "data.yml"
                    )
            );

        } catch (java.io.IOException e) {

            getLogger().severe(
                    "Gagal menyimpan data.yml: " +
                            e.getMessage()
            );
        }
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public AuraSkillsApi getAuraSkills() {
        return auraSkills;
    }

    public ClassManager getClassManager() {
        return classManager;
    }

    public ClassMenu getClassMenu() {
        return classMenu;
    }
}
