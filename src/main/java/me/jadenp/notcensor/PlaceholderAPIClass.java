package me.jadenp.notcensor;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

public class PlaceholderAPIClass {

    private PlaceholderAPIClass(){}

    private static boolean papiEnabled = false;

    public static void setPapiEnabled(boolean papiEnabled) {
        PlaceholderAPIClass.papiEnabled = papiEnabled;
    }

    public static String parse(String text, OfflinePlayer player) {
        if (papiEnabled) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }
}
