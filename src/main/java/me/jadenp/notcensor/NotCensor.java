package me.jadenp.notcensor;

import github.scarsz.discordsrv.DiscordSRV;
import me.jadenp.notcensor.listeners.ChatListener;
import me.jadenp.notcensor.listeners.DiscordSRVHook;
import me.jadenp.notcensor.listeners.EssentialsXHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class NotCensor extends JavaPlugin {

    public static final String VIEW_PERMISSION = "notcensor.view";
    public static final String BYPASS_PERMISSION = "notcensor.bypass";
    public static final String ADMIN_PERMISSION = "notcensor.admin";

    private boolean discordSRVEnabled;
    private DiscordSRVHook discordSRVHook;

    @Override
    public void onEnable() {
        // Plugin startup logic
        reloadFiles();

        CensorManager.setPlugin(this);

        Objects.requireNonNull(getCommand("NotCensor")).setExecutor(new Commands(this));
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);

        if (Bukkit.getPluginManager().isPluginEnabled("Essentials"))
            Bukkit.getPluginManager().registerEvents(new EssentialsXHook(), this);

        discordSRVEnabled = Bukkit.getPluginManager().isPluginEnabled("DiscordSRV");
        if (discordSRVEnabled) {
            discordSRVHook = new DiscordSRVHook();
            DiscordSRV.api.subscribe(discordSRVHook);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (discordSRVEnabled)
            DiscordSRV.api.unsubscribe(discordSRVHook);
    }

    /**
     * Reload the config.yml and censor_list.txt files
     */
    public void reloadFiles() {
        new ConfigOptions(this);
    }

}
