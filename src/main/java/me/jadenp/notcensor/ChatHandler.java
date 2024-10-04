package me.jadenp.notcensor;

import me.jadenp.notcensor.wrappers.CensorableMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

/**
 * This class is used to handle the chat options in the config.
 */
public class ChatHandler {
    private static boolean mentionPlayers;
    private static boolean hoverInformation;
    private static List<String> text;

    private ChatHandler(){}

    /**
     * Reads a chat message for a player mention
     * @param message
     */
    public static void handleMessage(CensorableMessage message) {
        if (!message.getMessage().contains("@"))
            // no mention
            return;
        Set<String> mentionedNames = new HashSet<>();
        String messageCopy = message.getMessage();
        while (messageCopy.contains("@") && messageCopy.indexOf("@") != messageCopy.length()-1) {
            String name = messageCopy.substring(messageCopy.indexOf("@") + 1);
            if (name.contains(" "))
                name = name.substring(0, name.indexOf(" "));
            mentionedNames.add(name);
            messageCopy = messageCopy.substring(0, messageCopy.indexOf("@")) + messageCopy.substring(messageCopy.indexOf("@") + name.length() + 1);
        }
        // iterate through all recipients
        Iterator<Player> playerIterator = message.getRecipients().iterator();
        while (playerIterator.hasNext()) {
            Player currPlayer = playerIterator.next();
            // check if message contains their @username or @displayName

        }
    }

}
