package me.jadenp.notcensor;

import me.jadenp.notcensor.wrappers.CensorableMessage;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

/**
 * This class is used to handle the chat options in the config.
 */
public class ChatHandler {
    private static boolean mentionPlayers;
    private static boolean hoverInformation;
    private static List<String> hoverText;
    private static ChatColor mentionColor;
    private static String mentionString;

    private ChatHandler(){}

    /**
     * Parses a chat message for any player mentions
     * @param message Message to parse.
     */
    public static void handleMessage(CensorableMessage message) {
        // <TODO>Use this function in place of sending messages to players, or return a string<\TODO>
        if (!mentionPlayers || !message.getMessage().contains("@"))
            // no mention
            return;
        Set<String> mentionedNames = getMentionedNames(message);
        // iterate through all recipients
        Iterator<Player> playerIterator = message.getRecipients().iterator();
        while (playerIterator.hasNext()) {
            Player currPlayer = playerIterator.next();
            // check if message contains their @username or @displayName
            if (mentionedNames.contains(currPlayer.getName().toLowerCase()) || mentionedNames.contains(ChatColor.stripColor(currPlayer.getDisplayName()).toLowerCase())) {
                playerIterator.remove(); // remove them from receiving the sent message
                // create the new message with the highlighted name
                String editedMessage = highlightMentionedName(highlightMentionedName(message.getMessage(), currPlayer.getName()), ChatColor.stripColor(currPlayer.getDisplayName()));
                // send the message
                message.sendPseudoMessage(editedMessage, Collections.singleton(currPlayer));
            }
        }
    }

    private static Set<String> getMentionedNames(CensorableMessage message) {
        Set<String> mentionedNames = new HashSet<>();
        String messageCopy = message.getMessage();
        while (messageCopy.contains(mentionString) && messageCopy.indexOf(mentionString) != messageCopy.length()-1) {
            String name = messageCopy.substring(messageCopy.indexOf(mentionString) + mentionString.length());
            if (name.contains(" "))
                name = name.substring(0, name.indexOf(" "));
            mentionedNames.add(name.toLowerCase());
            messageCopy = messageCopy.substring(0, messageCopy.indexOf(mentionString)) + messageCopy.substring(messageCopy.indexOf("@") + name.length() + 1);
        }
        return mentionedNames;
    }

    /**
     * Prefixes any substring containing @name with the mention string, and suffixes them with the last chat colors used.
     * @param message Message containing a mention.
     * @param name Name to be replaced.
     * @return The message but with @name as mentionColor.
     */
    private static String highlightMentionedName(String message, String name) {
        StringBuilder finalMessage = new StringBuilder();
        while (message.toLowerCase().contains(mentionString + name.toLowerCase())) {
            // get the substring before the name
            String beforeName = message.substring(0, message.toLowerCase().indexOf(mentionString + name.toLowerCase()));
            String caseSensitiveName = message.substring(beforeName.length(), beforeName.length() + mentionString.length() + name.length());
            String lastColors = ChatColor.getLastColors(beforeName);
            // set message to the substring after the name
            message = message.substring(beforeName.length() + caseSensitiveName.length());
            // add processed text to the final message
            finalMessage.append(beforeName).append(mentionColor).append(mentionString).append(caseSensitiveName).append(lastColors);
        }
        // add the rest of the message
        finalMessage.append(message);
        return finalMessage.toString();
    }

    /**
     * Get a hover event for a player's message.
     * @param player Player to parse the event for.
     * @return A hover event for the player.
     */
    public static HoverEvent getHoverEvent(Player player) {
        if (!hoverInformation)
            return null;
        StringBuilder builder = new StringBuilder();
        for (String text : hoverText) {
            builder.append(color(parsePlaceholders(text, player))).append("\n");
        }
        builder.deleteCharAt(builder.length()-1); // delete the last new line
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(builder.toString()));
    }

    /**
     * Parse PlaceholderAPI placeholders and internal placeholders.
     * @param text Text to be parsed.
     * @param player Player to parse the placeholders for.
     * @return The parsed text.
     */
    private static String parsePlaceholders(String text, OfflinePlayer player) {
        String playerName = player.getName() != null ? player.getName() : "Player";
        String playerDisplayName = player.isOnline() ? Objects.requireNonNull(player.getPlayer()).getDisplayName() : "Player";
        return PlaceholderAPIClass.parse(text
                        .replace("{name}", playerName)
                        .replace("{displayName}", playerDisplayName),
                player);
    }

    /**
     * Colors a string with chat colors and hex.
     * The hex can be in the form &#ffffff or <#ffffff></#ffffff>
     * @param str String to color.
     * @return The colored string.
     */
    private static String color(String str){
        str = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', str);
        return cancelColorCodes(translateHexColorCodes("<#", ">", translateHexColorCodes("&#","", str)));
    }
    private static String translateHexColorCodes(String startTag, String endTag, String message) {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find())
        {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }

    private static String cancelColorCodes(String message) {
        return message.replaceAll("</#" + "([A-Fa-f0-9]{6})" + ">", ChatColor.RESET + "");
    }

    public static void setMentionPlayers(boolean mentionPlayers) {
        ChatHandler.mentionPlayers = mentionPlayers;
    }

    public static void setMentionColor(String color) {
        ChatHandler.mentionColor = ChatColor.getByChar(color);
    }

    public static void setHoverInformation(boolean hoverInformation) {
        ChatHandler.hoverInformation = hoverInformation;
    }

    public static void setHoverText(List<String> hoverText) {
        ChatHandler.hoverText = hoverText;
    }

    public static void setMentionString(String mentionString) {
        ChatHandler.mentionString = mentionString;
    }

    public static boolean isHoverInformation() {
        return hoverInformation;
    }
}
