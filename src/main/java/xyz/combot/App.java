package xyz.combot;

import org.bukkit.plugin.java.JavaPlugin;

import xyz.combot.events.ThrowableHitSurface;

public class App extends JavaPlugin {

    public static String PluginName = "Bounceable";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new ThrowableHitSurface(), this);

        saveDefaultConfig();
    }
}
