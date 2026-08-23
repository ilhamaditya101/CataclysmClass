package com.cataclysm.classplugin.manager;

import com.cataclysm.classplugin.CataclysmClassPlugin;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class ClassManager {

    private final CataclysmClassPlugin plugin;
    private final LuckPerms luckPerms;

    public ClassManager(
            CataclysmClassPlugin plugin,
            LuckPerms luckPerms
    ) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    public void setClass(Player player, String classId) {

        String newGroup = plugin.getConfig()
                .getString("classes." + classId + ".group");

        if (newGroup == null || newGroup.isEmpty()) {
            return;
        }

        User user = luckPerms.getUserManager()
                .getUser(player.getUniqueId());

        if (user == null) {
            return;
        }

        /*
         * PERMANENT CLASS:
         * Kalau player sudah punya salah satu class,
         * jangan izinkan set class lagi.
         */
        if (getClass(player) != null) {
            return;
        }

        /*
         * Tambahkan group class.
         */
        user.data().add(
                Node.builder("group." + newGroup).build()
        );

        luckPerms.getUserManager().saveUser(user);
    }

    public String getClass(Player player) {

        User user = luckPerms.getUserManager()
                .getUser(player.getUniqueId());

        if (user == null) {
            return null;
        }

        var section = plugin.getConfig()
                .getConfigurationSection("classes");

        if (section == null) {
            return null;
        }

        /*
         * CEK GROUP CLASS SECARA LANGSUNG.
         *
         * Tidak menggunakan primaryGroup lagi.
         */
        for (String id : section.getKeys(false)) {

            String group = plugin.getConfig()
                    .getString("classes." + id + ".group");

            if (group == null || group.isEmpty()) {
                continue;
            }

            String groupNode = "group." + group;

            boolean hasClassGroup = user.getNodes()
                    .stream()
                    .anyMatch(node ->
                            node.getKey().equalsIgnoreCase(groupNode)
                    );

            if (hasClassGroup) {
                return id;
            }
        }

        return null;
    }

    public boolean hasClass(Player player) {
        return getClass(player) != null;
    }
}
