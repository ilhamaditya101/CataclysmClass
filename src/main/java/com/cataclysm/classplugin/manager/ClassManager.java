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

        Set<String> classGroups = new HashSet<>();

        var section = plugin.getConfig()
                .getConfigurationSection("classes");

        if (section != null) {

            for (String id : section.getKeys(false)) {

                String group = plugin.getConfig()
                        .getString("classes." + id + ".group");

                if (group != null && !group.isEmpty()) {
                    classGroups.add(group);
                }
            }
        }

        /*
         * Hapus group class lama.
         */
        for (String group : classGroups) {

            user.data().remove(
                    Node.builder("group." + group).build()
            );
        }

        /*
         * Tambahkan group class baru.
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

        String primaryGroup = user.getPrimaryGroup();

        var section = plugin.getConfig()
                .getConfigurationSection("classes");

        if (section == null) {
            return null;
        }

        for (String id : section.getKeys(false)) {

            String group = plugin.getConfig()
                    .getString("classes." + id + ".group");

            if (group != null &&
                    group.equalsIgnoreCase(primaryGroup)) {

                return id;
            }
        }

        return null;
    }
}
