package com.cataclysm.classplugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.cataclysm.classplugin.manager.ClassManager;
import com.cataclysm.classplugin.gui.ClassMenu;
import com.cataclysm.classplugin.command.*;
import com.cataclysm.classplugin.listener.XpListener;
public class CataclysmClassPlugin extends JavaPlugin {
 public void onEnable(){saveDefaultConfig();saveResource("data.yml",false);ClassManager cm=new ClassManager(this);ClassMenu menu=new ClassMenu(cm);getCommand("class").setExecutor(new ClassCommand(menu,cm));getCommand("classadmin").setExecutor(new AdminCommand(this,cm));getServer().getPluginManager().registerEvents(menu,this);getServer().getPluginManager().registerEvents(new XpListener(cm),this);}
}
