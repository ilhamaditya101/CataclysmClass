package com.cataclysm.classplugin.manager;

import com.cataclysm.classplugin.CataclysmClassPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.entity.Player;

public class ClassManager {

    private final CataclysmClassPlugin plugin;
    private final LuckPerms luckPerms;

    public ClassManager(CataclysmClassPlugin plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    public void setClass(Player player, String classId) {

        // Sudah punya class = TIDAK BOLEH GANTI
        if (getClass(player) != null) {
            return;
        }

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
         * Cek group class secara langsung,
         * bukan primary group.
         */
        for (String id : section.getKeys(false)) {

            String group = plugin.getConfig()
                    .getString("classes." + id + ".group");

            if (group == null || group.isEmpty()) {
                continue;
            }

            String groupNode = "group." + group;

            boolean hasGroup = user.getNodes()
                    .stream()
                    .anyMatch(node ->
                            node.getKey().equalsIgnoreCase(groupNode)
                    );

            if (hasGroup) {
                return id;
            }
        }

        return null;
    }

    public boolean hasClass(Player player) {
        return getClass(player) != null;
    }
}
