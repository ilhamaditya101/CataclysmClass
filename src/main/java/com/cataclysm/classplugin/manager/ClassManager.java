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

    private void createDataFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe(
                        "Gagal membuat players.yml: " + e.getMessage()
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
                    "Gagal menyimpan players.yml: " + e.getMessage()
            );
        }
    }

    public String getClass(Player player) {
        return getClass(player.getUniqueId());
    }

    public String getClass(OfflinePlayer player) {
        return getClass(player.getUniqueId());
    }

    public String getClass(UUID uuid) {
        return data.getString("players." + uuid + ".class");
    }

    public boolean hasClass(Player player) {
        return getClass(player) != null;
    }

    public boolean hasClass(OfflinePlayer player) {
        return getClass(player) != null;
    }

    public boolean setClass(Player player, String classId) {

        if (player == null || classId == null || classId.isBlank()) {
            return false;
        }

        classId = classId.toLowerCase(Locale.ROOT);

        if (hasClass(player)) {
            return false;
        }

        ConfigurationSection section =
                plugin.getConfig().getConfigurationSection(
                        "classes." + classId
                );

        if (section == null) {
            plugin.getLogger().warning(
                    "Class '" + classId +
                            "' tidak ditemukan di config.yml."
            );
            return false;
        }

        data.set(
                "players." + player.getUniqueId() + ".class",
                classId
        );

        data.set(
                "players." + player.getUniqueId() + ".reset_pending",
                false
        );

        saveData();

        String group = section.getString("group");

        if (group != null && !group.isBlank()) {
            addLuckPermsGroup(player, group);
        }

        applyAuraSkillsStats(player, classId);
        executeClassCommands(player, classId);

        return true;
    }

    public boolean resetClass(OfflinePlayer player) {

        if (player == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        String oldClass = getClass(uuid);

        if (oldClass == null || oldClass.isBlank()) {
            if (!player.isOnline()) {
                data.set(
                        "players." + uuid + ".reset_pending",
                        true
                );
                saveData();
            }

            return false;
        }

        if (player.isOnline()) {
            Player online = player.getPlayer();

            if (online != null) {
                removeAuraSkillsStats(online, oldClass);
            }
        }

        String group = getClassGroup(oldClass);

        if (group != null && !group.isBlank()) {

            if (player.isOnline()) {
                Player online = player.getPlayer();

                if (online != null) {
                    removeLuckPermsGroup(online, group);
                }
            } else {
                removeLuckPermsGroupOffline(uuid, group);
            }
        }

        data.set(
                "players." + uuid + ".class",
                null
        );

        data.set(
                "players." + uuid + ".reset_pending",
                false
        );

        saveData();

        return true;
    }

    public void handlePendingReset(Player player) {

        UUID uuid = player.getUniqueId();

        boolean pending = data.getBoolean(
                "players." + uuid + ".reset_pending",
                false
        );

        if (!pending) {
            return;
        }

        String oldClass = getClass(uuid);

        if (oldClass != null) {
            removeAuraSkillsStats(player, oldClass);

            String group = getClassGroup(oldClass);

            if (group != null && !group.isBlank()) {
                removeLuckPermsGroup(player, group);
            }
        }

        data.set(
                "players." + uuid + ".class",
                null
        );

        data.set(
                "players." + uuid + ".reset_pending",
                false
        );

        saveData();
    }

    private void addLuckPermsGroup(
            Player player,
            String group
    ) {

        User user =
                luckPerms.getPlayerAdapter(Player.class)
                        .getUser(player);

        if (user == null) {
            return;
        }

        boolean exists =
                user.getInheritedGroups(user.getQueryOptions())
                        .stream()
                        .anyMatch(
                                g -> g.getName()
                                        .equalsIgnoreCase(group)
                        );

        if (!exists) {
            Node node =
                    Node.builder("group." + group).build();

            user.data().add(node);
            saveLuckPermsUser(user);
        }
    }

    private void removeLuckPermsGroup(
            Player player,
            String group
    ) {

        User user =
                luckPerms.getPlayerAdapter(Player.class)
                        .getUser(player);

        if (user == null) {
            return;
        }

        Node node =
                Node.builder("group." + group).build();

        user.data().remove(node);
        saveLuckPermsUser(user);
    }

    private void removeLuckPermsGroupOffline(
            UUID uuid,
            String group
    ) {

        luckPerms.getUserManager()
                .loadUser(uuid)
                .thenAccept(user -> {

                    Node node =
                            Node.builder("group." + group).build();

                    user.data().remove(node);
                    saveLuckPermsUser(user);
                })
                .exceptionally(error -> {

                    plugin.getLogger().warning(
                            "Gagal load LuckPerms user " +
                                    uuid + ": " +
                                    error.getMessage()
                    );

                    return null;
                });
    }

    private void saveLuckPermsUser(User user) {

        luckPerms.getUserManager()
                .saveUser(user)
                .exceptionally(error -> {

                    plugin.getLogger().warning(
                            "Gagal menyimpan LuckPerms user " +
                                    user.getUniqueId() + ": " +
                                    error.getMessage()
                    );

                    return null;
                });
    }

    public String getClassGroup(String classId) {

        if (classId == null) {
            return null;
        }

        return plugin.getConfig().getString(
                "classes." + classId + ".group"
        );
    }

    private void applyAuraSkillsStats(
            Player player,
            String classId
    ) {

        if (auraSkills == null) {
            return;
        }

        ConfigurationSection stats =
                plugin.getConfig().getConfigurationSection(
                        "classes." + classId + ".stats"
                );

        if (stats == null) {
            return;
        }

        SkillsUser user =
                auraSkills.getUser(player.getUniqueId());

        if (user == null) {
            plugin.getLogger().warning(
                    "AuraSkills user tidak ditemukan untuk " +
                            player.getName()
            );
            return;
        }

        for (String statName : stats.getKeys(false)) {

            double value = stats.getDouble(statName);

            if (value == 0) {
                continue;
            }

            Stats stat = parseStat(statName);

            if (stat == null) {
                plugin.getLogger().warning(
                        "AuraSkills stat tidak dikenal: " +
                                statName
                );
                continue;
            }

            String modifierName =
                    getModifierName(classId, statName);

            user.removeStatModifier(modifierName);

            user.addStatModifier(
                    new StatModifier(
                            modifierName,
                            stat,
                            value
                    )
            );
        }
    }

    private void removeAuraSkillsStats(
            Player player,
            String classId
    ) {

        if (auraSkills == null) {
            return;
        }

        ConfigurationSection stats =
                plugin.getConfig().getConfigurationSection(
                        "classes." + classId + ".stats"
                );

        if (stats == null) {
            return;
        }

        SkillsUser user =
                auraSkills.getUser(player.getUniqueId());

        if (user == null) {
            return;
        }

        for (String statName : stats.getKeys(false)) {

            String modifierName =
                    getModifierName(classId, statName);

            user.removeStatModifier(modifierName);
        }
    }

    private String getModifierName(
            String classId,
            String statName
    ) {

        return MODIFIER_PREFIX +
                classId.toLowerCase(Locale.ROOT) +
                "." +
                statName.toLowerCase(Locale.ROOT);
    }

    private Stats parseStat(String name) {

        if (name == null) {
            return null;
        }

        switch (name.toLowerCase(Locale.ROOT)) {

            case "health":
                return Stats.HEALTH;

            case "strength":
                return Stats.STRENGTH;

            case "regeneration":
                return Stats.REGENERATION;

            case "luck":
                return Stats.LUCK;

            case "wisdom":
                return Stats.WISDOM;

            case "toughness":
                return Stats.TOUGHNESS;

            default:
                return null;
        }
    }

    private void executeClassCommands(
            Player player,
            String classId
    ) {

        List<String> commands =
                plugin.getConfig().getStringList(
                        "classes." + classId + ".commands"
                );

        if (commands.isEmpty()) {
            return;
        }

        for (String command : commands) {

            if (command == null || command.isBlank()) {
                continue;
            }

            command = command
                    .replace("{player}", player.getName())
                    .replace(
                            "{uuid}",
                            player.getUniqueId().toString()
                    )
                    .replace("{class}", classId);

            if (command.startsWith("/")) {
                command = command.substring(1);
            }

            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command
            );
        }
    }

    public void reload() {
        loadData();
    }

    public boolean classExists(String classId) {

        if (classId == null) {
            return false;
        }

        return plugin.getConfig()
                .getConfigurationSection(
                        "classes." +
                                classId.toLowerCase(Locale.ROOT)
                ) != null;
    }

    public Set<String> getClassIds() {

        ConfigurationSection section =
                plugin.getConfig()
                        .getConfigurationSection("classes");

        if (section == null) {
            return Collections.emptySet();
        }

        return section.getKeys(false);
    }
}               
