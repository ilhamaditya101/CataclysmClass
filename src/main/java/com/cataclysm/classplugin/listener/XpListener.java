package com.cataclysm.classplugin.listener;

import java.util.Locale;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import dev.aurelium.auraskills.api.event.skill.XpGainEvent;

import com.cataclysm.classplugin.manager.ClassManager;

public class XpListener implements Listener {

    private final ClassManager c;

    public XpListener(ClassManager c) {
        this.c = c;
    }

    @EventHandler
    public void xp(XpGainEvent e) {
        String cl = c.get(e.getPlayer());

        if (cl == null) {
            return;
        }

        String skill = e.getSkill().getId().toString().toLowerCase(Locale.ROOT);

        double m = c.mult(cl, skill);

        if (m != 1) {
            e.setAmount(e.getAmount() * m);
        }
    }
}
