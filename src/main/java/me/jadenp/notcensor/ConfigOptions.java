package me.jadenp.notcensor;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ConfigOptions {
    private boolean removeCensoredMessages; // Whether messages with censored words are removed entirely
    private int replacementPercent; // If remove-censored-messages is set to false, this controls how much of the censored words are removed
    // A message will be sent to the players with the notcensor.admin permission for censored messages
    private boolean censorNotificationEnabled;
    private String censorNotificationMessage;

    private Set<String> censoredWords;

    public ConfigOptions(NotCensor notCensor) {
        notCensor.saveDefaultConfig(); // save config if one doesn't exist already
        notCensor.reloadConfig(); // reload config if there are changes

        // load any missing options
        boolean madeChanges = false;
        notCensor.getConfig().setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(notCensor.getResource("config.yml")))));
        for (String key : Objects.requireNonNull(notCensor.getConfig().getDefaults()).getKeys(true)) {
            if (!notCensor.getConfig().isSet(key)) {
                notCensor.getConfig().set(key, notCensor.getConfig().getDefaults().get(key));
                madeChanges = true;
            }
        }
        // save config if any options were added
        if (madeChanges)
            notCensor.saveConfig();

        readConfig(notCensor.getConfig()); // read config options and store to variables

        readCensorList(notCensor); // read censor_list.txt file and load all words into censoredWords
    }

    private void readConfig(FileConfiguration config) {
        removeCensoredMessages = config.getBoolean("remove-censored-messages");
        replacementPercent = config.getInt("replacement-percent");
        censorNotificationEnabled = config.getBoolean("censor-notification.enabled");
        censorNotificationMessage = config.getString("censor-notification.message");
    }

    private void readCensorList(NotCensor notCensor) {
        File censorFile = new File(notCensor.getDataFolder() + File.separator + "censor_list.txt");
        // load resource if it doesn't exist already
        if (!censorFile.exists())
            notCensor.saveResource("censor_list.txt", false);

        censoredWords = new HashSet<>();
        // read through each line in the file
        try (BufferedReader reader = new BufferedReader(new FileReader(censorFile))) {
            String word = reader.readLine();

            while (word != null) {
                censoredWords.add(word.toLowerCase());
                word = reader.readLine();
            }
        } catch (IOException e) {
            // error reading file
            Bukkit.getLogger().warning("[NotCensor] Error reading the censor list!");
            Bukkit.getLogger().warning(e.toString());
        }
    }

    public boolean isCensorNotificationEnabled() {
        return censorNotificationEnabled;
    }

    public boolean isRemoveCensoredMessages() {
        return removeCensoredMessages;
    }

    public int getReplacementPercent() {
        return replacementPercent;
    }

    public String getCensorNotificationMessage() {
        return censorNotificationMessage;
    }

    public Set<String> getCensoredWords() {
        return censoredWords;
    }
}
