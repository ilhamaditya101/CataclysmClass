package com.cataclysm.classplugin.manager;

import com.cataclysm.classplugin.CataclysmClassPlugin;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.stat.Stats;
import dev.aurelium.auraskills.api.user.SkillsUser;
import dev.aurelium.auraskills.api.stat.modifier.StatModifier;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class ClassManager {

    private final CataclysmClassPlugin plugin;
    private final LuckPerms luckPerms;
    private final AuraSkillsApi auraSkills;

    private final File dataFile;
    private FileConfiguration data;

    /*
     * Prefix untuk semua AuraSkills modifier milik CataclysmClass.
     *
     * Contoh:
     * cataclysmclass.fighter.strength
     * cataclysmclass.fighter.health
     */
    private static final String MODIFIER_PREFIX = "cataclysmclass.";

    public ClassManager(
            CataclysmClassPlugin plugin,
            LuckPerms luckPerms,
            AuraSkillsApi auraSkills
    ) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.auraSkills = auraSkills;

        this.dataFile = new File(plugin.getDataFolder(), "players.yml");

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

    // =========================================================
    // GET CLASS
    // =========================================================

    /**
     * Mendapatkan ID class player.
     *
     * @return class ID atau null kalau belum punya class.
     */
    public String getClass(Player player) {
        return getClass(player.getUniqueId());
    }

    public String getClass(OfflinePlayer player) {
        return getClass(player.getUniqueId());
    }

    public String getClass(UUID uuid) {
        return data.getString("players." + uuid + ".class");
    }

    /**
     * Mengecek apakah player sudah punya class.
     */
    public boolean hasClass(Player player) {
        return getClass(player) != null;
    }

    public boolean hasClass(OfflinePlayer player) {
        return getClass(player) != null;
    }

    // =========================================================
    // SET CLASS
    // =========================================================

    /**
     * Memilih class untuk player.
     *
     * Return true  = berhasil
     * Return false = gagal / sudah punya class / class tidak ditemukan
     */
    public boolean setClass(Player player, String classId) {

        if (player == null || classId == null || classId.isBlank()) {
            return false;
        }

        classId = classId.toLowerCase(Locale.ROOT);

        /*
         * Class hanya bisa dipilih satu kali.
         */
        if (hasClass(player)) {
            return false;
        }

        ConfigurationSection classSection =
                plugin.getConfig().getConfigurationSection(
                        "classes." + classId
                );

        if (classSection == null) {
            plugin.getLogger().warning(
                    "Class '" + classId + "' tidak ditemukan di config.yml."
            );
            return false;
        }

        /*
         * Simpan class.
         */
        data.set(
                "players." + player.getUniqueId() + ".class",
                classId
        );

        data.set(
                "players." + player.getUniqueId() + ".reset_pending",
                false
        );

        saveData();

        /*
         * Tambahkan LuckPerms group.
         */
        String group = classSection.getString("group");

        if (group != null && !group.isBlank()) {
            addLuckPermsGroup(player, group);
        }

        /*
         * Tambahkan AuraSkills stats.
         */
        applyAuraSkillsStats(player, classId);

        /*
         * Jalankan reward / command class.
         */
        executeClassCommands(player, classId);

        return true;
    }

    // =========================================================
    // RESET CLASS
    // =========================================================

    /**
     * Reset class player.
     *
     * Method ini sengaja menerima OfflinePlayer karena command admin
     * dapat mereset player yang sedang offline.
     */
    public boolean resetClass(OfflinePlayer player) {

        if (player == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();

        String oldClass = getClass(uuid);

        /*
         * Kalau tidak punya class, tidak ada yang perlu di-reset.
         */
        if (oldClass == null || oldClass.isBlank()) {

            /*
             * Tetap tandai reset pending jika offline agar group LP
             * bisa dibersihkan ketika player login.
             */
            if (!player.isOnline()) {
                data.set(
                        "players." + uuid + ".reset_pending",
                        true
                );
                saveData();
            }

            return false;
        }

        /*
         * Hapus AuraSkills modifier kalau player online.
         */
        if (player.isOnline()) {
            Player online = player.getPlayer();

            if (online != null) {
                removeAuraSkillsStats(online, oldClass);
            }
        }

        /*
         * Hapus LuckPerms group.
         *
         * Kalau online, langsung.
         * Kalau offline, async load User LuckPerms.
         */
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

        /*
         * Hapus data class.
         */
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

    // =========================================================
    // PENDING RESET
    // =========================================================

    /**
     * Dipanggil ketika player login.
     *
     * Ini menangani reset player yang sebelumnya offline.
     */
    public void handlePendingReset(Player player) {

        UUID uuid = player.getUniqueId();

        boolean pending = data.getBoolean(
                "players." + uuid + ".reset_pending",
                false
        );

        if (!pending) {
            return;
        }

        /*
         * Kalau masih ada class, bersihkan modifier.
         */
        String oldClass = getClass(uuid);

        if (oldClass != null) {
            removeAuraSkillsStats(player, oldClass);

            String group = getClassGroup(oldClass);

            if (group != null && !group.isBlank()) {
                removeLuckPermsGroup(player, group);
            }
        }

        /*
         * Bersihkan data.
         */
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

    // =========================================================
    // LUCKPERMS
    // =========================================================

    private void addLuckPermsGroup(Player player, String group) {

        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);

        if (user == null) {
            return;
        }

        /*
         * Hanya tambahkan group kalau belum punya.
         */
        if (!user.getInheritedGroups(user.getQueryOptions())
                .stream()
                .anyMatch(g -> g.getName().equalsIgnoreCase(group))) {

            Node node = Node.builder("group." + group).build();

            user.data().add(node);

            saveLuckPermsUser(user);
        }
    }

    private void removeLuckPermsGroup(Player player, String group) {

        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);

        if (user == null) {
            return;
        }

        Node node = Node.builder("group." + group).build();

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

                    Node node = Node.builder(
                            "group." + group
                    ).build();

                    user.data().remove(node);

                    saveLuckPermsUser(user);
                })
                .exceptionally(error -> {

                    plugin.getLogger().warning(
                            "Gagal load LuckPerms user " +
                                    uuid +
                                    ": " +
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
                                    user.getUniqueId() +
                                    ": " +
                                    error.getMessage()
                    );

                    return null;
                });
    }

    // =========================================================
    // CLASS GROUP
    // =========================================================

    public String getClassGroup(String classId) {

        if (classId == null) {
            return null;
        }

        return plugin.getConfig().getString(
                "classes." + classId + ".group"
        );
    }

    // =========================================================
    // AURASKILLS
    // =========================================================

    /**
     * Membaca:
     *
     * classes:
     *   fighter:
     *     stats:
     *       strength: 10
     *       health: 5
     *
     * Stat dapat kamu tambah / hapus sesuka hati.
     */
    private void applyAuraSkillsStats(
            Player player,
            String classId
    ) {

        if (auraSkills == null) {
            return;
        }

        ConfigurationSection statsSection =
                plugin.getConfig().getConfigurationSection(
                        "classes." + classId + ".stats"
                );

        if (statsSection == null) {
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

        for (String statName : statsSection.getKeys(false)) {

            double value =
                    statsSection.getDouble(statName);

            if (value == 0) {
                continue;
            }

            Stats stat = parseStat(statName);

            if (stat == null) {
                plugin.getLogger().warning(
                        "AuraSkills stat tidak dikenal: " +
                                statName +
                                " pada class " +
                                classId
                );
                continue;
            }

            String modifierName =
                    getModifierName(classId, statName);

            /*
             * Pastikan modifier lama tidak menumpuk.
             */
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

    /**
     * Hapus seluruh stat modifier milik class.
     */
    private void removeAuraSkillsStats(
            Player player,
            String classId
    ) {

        if (auraSkills == null) {
            return;
        }

        ConfigurationSection statsSection =
                plugin.getConfig().getConfigurationSection(
                        "classes." + classId + ".stats"
                );

        if (statsSection == null) {
            return;
        }

        SkillsUser user =
                auraSkills.getUser(player.getUniqueId());

        if (user == null) {
            return;
        }

        for (String statName : statsSection.getKeys(false)) {

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

    /**
     * Parse nama stat dari config.
     *
     * AuraSkills 2.2 default stats:
     * health
     * strength
     * regeneration
     * luck
     * wisdom
     * toughness
     */
    private Stats parseStat(String name) {

        if (name == null) {
            return null;
        }

        return switch (
                name.toLowerCase(Locale.ROOT)
        ) {

            case "health" ->
                    Stats.HEALTH;

            case "strength" ->
                    Stats.STRENGTH;

            case "regeneration" ->
                    Stats.REGENERATION;

            case "luck" ->
                    Stats.LUCK;

            case "wisdom" ->
                    Stats.WISDOM;

            case "toughness" ->
                    Stats.TOUGHNESS;

            default ->
                    null;
        };
    }

    // =========================================================
    // CLASS COMMANDS / REWARDS
    // =========================================================

    /**
     * Optional:
     *
     * classes:
     *   fighter:
     *     commands:
     *       - "give {player} iron_sword 1"
     *       - "say {player} memilih Fighter"
     *
     * Command dijalankan dari console.
     */
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
                    .replace("{uuid}", player.getUniqueId().toString())
                    .replace("{class}", classId);

            /*
             * Jangan pakai slash di console.
             */
            if (command.startsWith("/")) {
                command = command.substring(1);
            }

            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command
            );
        }
    }

    // =========================================================
    // RELOAD
    // =========================================================

    public void reload() {
        loadData();
    }

    // =========================================================
    // UTILITY
    // =========================================================

    /**
     * Dipakai kalau ingin mengecek apakah class tersedia.
     */
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

    /**
     * Mendapatkan semua class ID.
     */
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
