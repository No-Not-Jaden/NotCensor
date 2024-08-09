package me.jadenp.notcensor;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class ChatListener implements Listener {
    private final NotCensor notCensor;
    private final Random random;

    public ChatListener(NotCensor notCensor) {
        this.notCensor = notCensor;
        random = new Random(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer().hasPermission(NotCensor.BYPASS_PERMISSION))
            return;
        if (shouldMessageBeCensored(event.getMessage())) {
            // censor
            if (notCensor.getConfigOptions().isRemoveCensoredMessages()) {
                // remove message
                removeMessage(event);
            } else {
                // modify message
                censorMessage(event);
            }
        }
    }

    private void censorMessage(AsyncPlayerChatEvent event) {
        String message = starMessage(event.getMessage());

        Set<Player> recipientsCopy = new HashSet<>(event.getRecipients()); // this is for constructing a new recipient list to send players a new message
        Set<Player> allRecipients = new HashSet<>(event.getRecipients()); // this is for notifying admins later on
        try {
            if (event.getRecipients().removeIf(player -> !player.hasPermission(NotCensor.VIEW_PERMISSION) && !player.equals(event.getPlayer()))) {
                // recipients were removed
                // this event will be for the players without the notcensor.view permission
                // a new event will be called with the censored message
                // the output of that event will be sent to all the recipients

                // remove any players that will have already seen this message
                recipientsCopy.removeIf(player -> event.getRecipients().contains(player));
                // call new event to be manually sent to everyone by this plugin
                String playerMessage = getFormattedPlayerMessage(event.getPlayer(), message, recipientsCopy);
                for (Player player : recipientsCopy) {
                    player.sendMessage(playerMessage);
                }
            } else {
                // message doesn't need to be changed
                return;
            }
        } catch (UnsupportedOperationException ignored) {
            // cannot modify the recipients in this set
            // cancel event and send messages manually
            event.setCancelled(true);

            Set<Player> censoredRecipients = new HashSet<>(recipientsCopy);
            censoredRecipients.removeIf(player -> !player.hasPermission(NotCensor.VIEW_PERMISSION) && !player.equals(event.getPlayer())); // remove anyone that won't have a censored message
            recipientsCopy.removeIf(censoredRecipients::contains); // remove everyone that wasn't removed from the censored list

            String unCensoredMessage = getFormattedPlayerMessage(event.getPlayer(), event.getMessage(), recipientsCopy);
            recipientsCopy.forEach(player -> player.sendMessage(unCensoredMessage));
            String censoredMessage = getFormattedPlayerMessage(event.getPlayer(), message, censoredRecipients);
            censoredRecipients.forEach(player -> player.sendMessage(censoredMessage));
        }

        if (notCensor.getConfigOptions().isCensorNotificationEnabled()) {
            // send a notification to all admins getting this message
            for (Player player : allRecipients) {
                if (player.hasPermission(NotCensor.ADMIN_PERMISSION) && !player.equals(event.getPlayer()))
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', notCensor.getConfigOptions().getCensorNotificationMessage()));
            }
        }
    }

    /**
     * Checks the message for each censored word and replaces it with a censored version
     * @param message Message to be checked
     * @return A censored message
     */
    private String starMessage(String message) {
        String lowercaseMessage = message.toLowerCase(Locale.ROOT);
        for (String word : notCensor.getConfigOptions().getCensoredWords()) {
            while (lowercaseMessage.equals(word) || lowercaseMessage.startsWith(word + " ") || lowercaseMessage.endsWith(" " + word) || lowercaseMessage.contains(" " + word + " ")) {
                String caseSensitiveWord = getCaseSensitiveWord(message, word, lowercaseMessage);
                String censoredWord = starWord(caseSensitiveWord); // censor word
                // censor in both messages
                message = message.replace(caseSensitiveWord, censoredWord);
                if (word.equals(censoredWord)) {
                    // replace censoredWord with placeholder stars, so it doesn't try to replace it again
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < word.length(); i++) {
                        builder.append('*');
                    }
                    censoredWord = builder.toString();
                }
                lowercaseMessage = lowercaseMessage.replace(word, censoredWord);
            }
        }
        return message;
    }

    private static String getCaseSensitiveWord(String message, String word, String lowercaseMessage) {
        int index = lowercaseMessage.indexOf(" " + word + " ") + 1;
        if (index == 0)
            index = lowercaseMessage.indexOf(" " + word) + 1;
        if (index == 0)
            index = lowercaseMessage.indexOf(word + " ");
        if (index == -1)
            index = lowercaseMessage.indexOf(word);
        return message.substring(index, index + word.length());
    }

    /**
     * Censors a word with stars '*'
     * @param word Word to be censored
     * @return a censored word
     */
    private String starWord(String word) {
        double censorPercent = (double) notCensor.getConfigOptions().getReplacementPercent() / 100;
        StringBuilder builder = new StringBuilder();
        if (censorPercent >= 1) {
            // individual case for complete starring without any calculations
            for (int i = 0; i < word.length(); i++) {
                builder.append('*');
            }
        } else if (censorPercent < 0) {
            // individual case for random starring
            for (int i = 0; i < random.nextInt(3, 8); i++) {
                builder.append('*');
            }
        } else if (censorPercent == 0) {
            // individual case for no starring
            return word;
        } else if (censorPercent == 0.01) {
            // individual case for 1 star
            for (int i = 0; i < word.length(); i++) {
                if (i == 1)
                    builder.append('*');
                else
                    builder.append(word.charAt(i));
            }
        } else {
            return starCenter(word, censorPercent);
        }
        return builder.toString();
    }

    private String starCenter(String word, double censorPercent) {
        StringBuilder builder = new StringBuilder();
        // star percent of word with a minimum of 1 un-starred letter and 1 starred letter
        int starredLetters = (int) (word.length() * censorPercent);
        // make sure at least one letter is starred
        if (starredLetters == 0)
            starredLetters = 1;

        // this is the index of where to switch from not starring letters to starring letters
        // the goal is to star the center of the word
        int indexChange = word.length() / 2 - starredLetters / 2;
        if (indexChange == 0)
            indexChange = 1; // don't want the first letter to be changed
        for (int i = 0; i < word.length(); i++) {
            if (i >= indexChange && i < indexChange + starredLetters) {
                // add star
                builder.append('*');
            } else {
                // add letter
                builder.append(word.charAt(i));
            }
        }
        return builder.toString();
    }

    private String getFormattedPlayerMessage(Player player, String messageReplacement, Set<Player> recipients) {
        // call AsyncPlayerChatEvent manually to allow other plugins to modify the message
        AsyncPlayerChatEvent newMessage = new AsyncPlayerChatEvent(false, player, messageReplacement, recipients);
        return String.format(newMessage.getFormat(), player.getDisplayName(), newMessage.getMessage());
    }

    private void removeMessage(AsyncPlayerChatEvent event) {
        Set<Player> recipientsCopy = new HashSet<>(event.getRecipients());
        boolean anyRecipientsRemoved;
        try {
            // modify recipients to be only those with a view permission
            anyRecipientsRemoved = event.getRecipients().removeIf(player -> !player.hasPermission(NotCensor.VIEW_PERMISSION) && !player.equals(event.getPlayer()));
        } catch (UnsupportedOperationException e) {
            // can't remove recipients from this set
            event.setCancelled(true);

            // construct a new modifiable set with only the players that need to be sent a message
            Set<Player> uncensoredRecipients = new HashSet<>(event.getRecipients());
            anyRecipientsRemoved = uncensoredRecipients.removeIf(player -> !player.hasPermission(NotCensor.VIEW_PERMISSION) && !player.equals(event.getPlayer()));
            // call a new event and send message to desired players
            String message = getFormattedPlayerMessage(event.getPlayer(), event.getMessage(), uncensoredRecipients);
            // send message to all the recipients
            uncensoredRecipients.forEach(player -> player.sendMessage(message));
        }

        if (notCensor.getConfigOptions().isCensorNotificationEnabled() && anyRecipientsRemoved) {
            // notify admins
            String colorChangedMessage = ChatColor.GRAY + "" + ChatColor.ITALIC + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', event.getPlayer().getDisplayName() + " > " + event.getMessage()));
            for (Player player : recipientsCopy) {
                if (player.hasPermission(NotCensor.ADMIN_PERMISSION) && !player.equals(event.getPlayer())) {
                    // player is an admin
                    // send notification message
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', notCensor.getConfigOptions().getCensorNotificationMessage()));
                    if (!player.hasPermission(NotCensor.VIEW_PERMISSION)) {
                        // send color changed message
                        player.sendMessage(colorChangedMessage);
                    }
                }
            }
        }
    }

    private boolean shouldMessageBeCensored(String text) {
        text = text.toLowerCase(Locale.ROOT);
        for (String word :notCensor.getConfigOptions().getCensoredWords())
            if (text.equals(word) || text.startsWith(word + " ") || text.endsWith(" " + word) || text.contains(" " + word + " "))
                return true;
        return false;
    }
}
