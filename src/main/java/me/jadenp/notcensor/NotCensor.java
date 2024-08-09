package me.jadenp.notcensor;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class NotCensor extends JavaPlugin {

    private ConfigOptions configOptions;

    public static final String VIEW_PERMISSION = "notcensor.view";
    public static final String BYPASS_PERMISSION = "notcensor.bypass";
    public static final String ADMIN_PERMISSION = "notcensor.admin";

    @Override
    public void onEnable() {
        // Plugin startup logic
        reloadFiles();

        Objects.requireNonNull(getCommand("NotCensor")).setExecutor(new Commands(this));
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    /**
     * Reload the config.yml and censor_list.txt files
     */
    public void reloadFiles() {
        configOptions = new ConfigOptions(this);
    }

    public ConfigOptions getConfigOptions() {
        return configOptions;
    }

}
