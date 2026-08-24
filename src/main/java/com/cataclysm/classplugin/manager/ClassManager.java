package com.cataclysm.classplugin.manager;

import com.cataclysm.classplugin.CataclysmClassPlugin;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.stat.StatModifier;
import dev.aurelium.auraskills.api.stat.Stats;
import dev.aurelium.auraskills.api.user.SkillsUser;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class ClassManager {

    private final CataclysmClassPlugin plugin;
    private final LuckPerms luckPerms;
    private final AuraSkillsApi auraSkills;

    private final File dataFile;
    private FileConfiguration data;

    private static final String MODIFIER_PREFIX = "cataclysmclass.";

    public ClassManager(
            CataclysmClassPlugin plugin,
            LuckPerms luckPerms,
            AuraSkillsApi auraSkills
    ) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.auraSkills = auraSkills;

        this.dataFile = new File(
                plugin.getDataFolder(),
                "players.yml"
        );

        createDataFile();
        loadData();
    }

    // =========================================================
    // DATA
    // =========================================================

    private void createDataFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe(
                        "Gagal membuat players.yml: " +
                                e.getMessage()
                );
            }
        }
    }

    private void loadData() {
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe(
                    "Gagal menyimpan players.yml: " +
                            e.getMessage()
            );
        }
    }

    // =========================================================
    // GET CLASS
    // =========================================================

    public String getClass(Player player) {
        return getClass(player.getUniqueId());
    }

    public String getClass(OfflinePlayer player) {
        return getClass(player.getUniqueId());
    }

    public String getClass(UUID uuid) {
        return data.getString(
                "players." + uuid + ".class"
        );
    }

    public boolean hasClass(Player player) {
        return getClass(player) != null;
    }

    public boolean hasClass(OfflinePlayer player) {
        return getClass(player) != null;
    }

    // =========================================================
    // SET CLASS
    // =========================================================

    public boolean setClass(
            Player player,
            String classId
    ) {

        if (player == null ||
                classId == null ||
                classId.isBlank()) {

            return false;
        }

        classId = classId.toLowerCase(Locale.ROOT);

        if (hasClass(player)) {
            return false;
        }

        ConfigurationSection classSection =
                plugin.getConfig()
                        .getConfigurationSection(
                                "classes." + classId
                        );

        if (classSection == null) {
            plugin.getLogger().warning(
                    "Class '" +
                            classId +
                            "' tidak ditemukan di config.yml."
            );

            return false;
        }

        data.set(
                "players." +
                        player.getUniqueId() +
                        ".class",
                classId
        );

        data.set(
                "players." +
                        player.getUniqueId() +
                        ".reset_pending",
                false
        );

        saveData();

        String group =
                classSection.getString("group");

        if (group != null && !group.isBlank()) {
            addLuckPermsGroup(player, group);
        }

        applyAuraSkillsStats(
                player,
                classId
        );

        executeClassCommands(
                player,
                classId
        );

        return true;
    }

    // =========================================================
    // RESET CLASS
    // =========================================================

    public boolean resetClass(
            OfflinePlayer player
    ) {

        if (player == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();

        String oldClass = getClass(uuid);

        if (oldClass == null ||
                oldClass.isBlank()) {

            if (!player.isOnline()) {

                data.set(
                        "players." +
                                uuid +
                                ".reset_pending",
                        true
                );

                saveData();
            }

            return false;
        }

        if (player.isOnline()) {

            Player online = player.getPlayer();

            if (online != null) {
                removeAuraSkillsStats(
                        online,
                        oldClass
                );
            }
        }

        String group =
                getClassGroup(oldClass);

        if (group != null &&
                !group.isBlank()) {

            if (player.isOnline()) {

                Player online =
                        player.getPlayer();

                if (online != null) {
                    removeLuckPermsGroup(
                            online,
                            group
                    );
                }

            } else {

                removeLuckPermsGroupOffline(
                        uuid,
                        group
                );
            }
        }

        data.set(
                "players." +
                        uuid +
                        ".class",
                null
        );

        data.set(
                "players." +
                        uuid +
                        ".reset_pending",
                false
        );

        saveData();

        return true;
    }

    // =========================================================
    // PENDING RESET
    // =========================================================

    public void handlePendingReset(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        boolean pending =
                data.getBoolean(
                        "players." +
                                uuid +
                                ".reset_pending",
                        false
                );

        if (!pending) {
            return;
        }

        String oldClass =
                getClass(uuid);

        if (oldClass != null) {

            removeAuraSkillsStats(
                    player,
                    oldClass
            );

            String group =
                    getClassGroup(oldClass);

            if (group != null &&
                    !group.isBlank()) {

                removeLuckPermsGroup(
                        player,
                        group
                );
            }
        }

        data.set(
                "players." +
                        uuid +
                        ".class",
                null
        );

        data.set(
                "players." +
                        uuid +
                        ".reset_pending",
                false
        );

        saveData();
    }

    // =========================================================
    // LUCKPERMS
    // =========================================================

    private void addLuckPermsGroup(
            Player player,
            String group
    ) {

        User user =
                luckPerms
                        .getPlayerAdapter(Player.class)
                        .getUser(player);

        if (user == null) {
            return;
        }

        boolean alreadyHasGroup =
                user.getInheritedGroups(
                                user.getQueryOptions()
                        )
                        .stream()
                        .anyMatch(
                                g -> g.getName()
                                        .equalsIgnoreCase(group)
                        );

        if (!alreadyHasGroup) {

            Node node =
                    Node.builder(
                            "group." + group
                    ).build();

            user.data().add(node);

            saveLuckPermsUser(user);
        }
    }

    private void removeLuckPermsGroup(
            Player player,
            String group
    ) {

        User user =
                luckPerms
                        .getPlayerAdapter(Player.class)
                        .getUser(player);

        if (user == null) {
            return;
        }

        Node node =
                Node.builder(
                        "group." + group
                ).build();

        user.data().remove(node);

        saveLuckPermsUser(user);
    }

    private void removeLuckPermsGroupOffline(
            UUID uuid,
            String group
    ) {

        luckPerms.getUserManager()
               
