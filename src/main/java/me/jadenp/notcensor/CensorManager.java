package me.jadenp.notcensor;

import me.jadenp.notcensor.wrappers.CensorableMessage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CensorManager {
    private static final Random random = new Random(System.currentTimeMillis());

    private static boolean removeCensoredMessages; // Whether messages with censored words are removed entirely
    private static int replacementPercent; // If remove-censored-messages is set to false, this controls how much of the censored words are removed
    // A message will be sent to the players with the notcensor.admin permission for censored messages
    private static boolean censorNotificationEnabled;
    private static String censorNotificationMessage;

    private static Set<String> censoredWords;

    private static final Map<String, MessageRecord> lastMessages = new LinkedHashMap<>();
    private static long duplicateMessageInterval;
    private static final long DUPLICATE_MONITOR_INTERVAL = 200;

    private static NotCensor notCensor;

    private CensorManager(){}

    private enum MessageAction {
        CENSOR, IGNORE, DUPLICATE
    }

    public static void setCensorNotificationEnabled(boolean censorNotificationEnabled) {
        CensorManager.censorNotificationEnabled = censorNotificationEnabled;
    }

    public static void setCensoredWords(Set<String> censoredWords) {
        CensorManager.censoredWords = censoredWords;
    }

    public static void setCensorNotificationMessage(String censorNotificationMessage) {
        CensorManager.censorNotificationMessage = censorNotificationMessage;
    }

    public static void setRemoveCensoredMessages(boolean removeCensoredMessages) {
        CensorManager.removeCensoredMessages = removeCensoredMessages;
    }

    public static void setReplacementPercent(int replacementPercent) {
        CensorManager.replacementPercent = replacementPercent;
    }

    public static void setPlugin(NotCensor notCensor) {
        CensorManager.notCensor = notCensor;
    }

    public static void setDuplicateMessageInterval(long duplicateMessageInterval) {
        CensorManager.duplicateMessageInterval = duplicateMessageInterval;
        lastMessages.clear();
    }

    /**
     * Checks if a duplicate message was just sent.
     * If the message is a duplicate, a chat monitor, and a broadcast, the event will be canceled.
     * @param censorableMessage Censorable message to check.
     * @return {@link MessageAction#IGNORE} if the censorable message is a chat monitor and a duplicate message was recently sent. Otherwise, {@link MessageAction#DUPLICATE} if the message is a duplicate. {@link MessageAction#CENSOR} if the last message was different.
     */
    private static MessageAction duplicateMessageCheck(CensorableMessage censorableMessage) {
        if (lastMessages.containsKey(censorableMessage.getID())) {
            MessageRecord messageRecord = lastMessages.get(censorableMessage.getID());
            if (censorableMessage.isChatMonitor() && System.currentTimeMillis() - messageRecord.getTime() < DUPLICATE_MONITOR_INTERVAL && compareMessageRecord(censorableMessage.getMessage(), messageRecord)) {
                if (censorableMessage.isBroadcast())
                    censorableMessage.setCancelled(true);
                return MessageAction.IGNORE;
            }
            if (System.currentTimeMillis() - messageRecord.getTime() < duplicateMessageInterval && compareMessageRecord(censorableMessage.getMessage(), messageRecord))
                return MessageAction.DUPLICATE;
        }
        return MessageAction.CENSOR;
    }

    /**
     * @return True if the message is the same as the messageRecord
     */
    private static boolean compareMessageRecord(String message, MessageRecord messageRecord) {
        return messageRecord.getMessage().equals(message) || (messageRecord.getCensoredMessage() != null && messageRecord.getCensoredMessage().equals(message));
    }

    public static void processMessage(CensorableMessage censorableMessage) {
        if (censorableMessage.getPlayer() != null && censorableMessage.getPlayer().hasPermission(NotCensor.BYPASS_PERMISSION) || censorableMessage.isCancelled())
            return;
        MessageAction messageAction = duplicateMessageCheck(censorableMessage);
        if (messageAction == MessageAction.DUPLICATE) {
            removeMessage(censorableMessage, true);
        } else if (messageAction == MessageAction.CENSOR) {
            MessageRecord messageRecord = new MessageRecord(censorableMessage.getMessage());
            lastMessages.put(censorableMessage.getID(), messageRecord);
            if (shouldMessageBeCensored(censorableMessage.getMessage())) {
                // censor
                if (removeCensoredMessages) {
                    // remove message
                    removeMessage(censorableMessage, false);
                } else {
                    // modify message
                    censorMessage(censorableMessage, messageRecord);
                }
            }
        }
    }

    private static void censorMessage(CensorableMessage censorableMessage, MessageRecord messageRecord) {
        String message = starMessage(censorableMessage.getMessage());
        messageRecord.setCensoredMessage(message);

        Set<Player> recipientsCopy = new HashSet<>(censorableMessage.getRecipients()); // this is for constructing a new recipient list to send players a new message
        Set<Player> allRecipients = new HashSet<>(censorableMessage.getRecipients()); // this is for notifying admins later on
        try {
            if (censorableMessage.isBroadcast() || censorableMessage.getRecipients().removeIf(player -> !player.hasPermission(NotCensor.VIEW_PERMISSION) && !player.equals(censorableMessage.getPlayer()))) {
                // recipients were removed
                // this censorableMessage will be for the players without the notcensor.view permission
                // a new censorableMessage will be called with the censored message
                // the output of that censorableMessage will be sent to all the recipients

                // remove any players that will have already seen this message
                recipientsCopy.removeIf(player -> censorableMessage.getRecipients().contains(player));
                // call new censorableMessage to be manually sent to everyone by this plugin
                if (censorableMessage.isAsynchronous()) {
                   new BukkitRunnable() {
                       @Override
                       public void run() {
                           censorableMessage.sendPseudoMessage(message, recipientsCopy);
                       }
                   } .runTask(notCensor);
                } else {
                    censorableMessage.sendPseudoMessage(message, recipientsCopy);
                }
            } else {
                // message doesn't need to be changed
                return;
            }
        } catch (UnsupportedOperationException ignored) {
            // cannot modify the recipients in this set
            // cancel censorableMessage and send messages manually
            censorableMessage.setCancelled(true);

            Set<Player> censoredRecipients = new HashSet<>(censorableMessage.getRecipients());
            recipientsCopy.removeIf(player -> !player.hasPermission(NotCensor.VIEW_PERMISSION) && !player.equals(censorableMessage.getPlayer())); // remove anyone that should get a censored message
            censoredRecipients.removeIf(recipientsCopy::contains); // remove everyone that wasn't removed from the uncensored list

            if (censorableMessage.isAsynchronous()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        censorableMessage.sendPseudoMessage(censorableMessage.getMessage(), recipientsCopy);
                        censorableMessage.sendPseudoMessage(message, censoredRecipients);
                    }
                }.runTask(notCensor);
            } else {
                censorableMessage.sendPseudoMessage(censorableMessage.getMessage(), recipientsCopy);
                censorableMessage.sendPseudoMessage(message, censoredRecipients);
            }
        }

        if (censorNotificationEnabled) {
            // send a notification to all admins getting this message
            sendSyncCensorNotification(censorableMessage, allRecipients);
        }
    }

    private static void sendSyncCensorNotification(CensorableMessage censorableMessage, Set<Player> recipients) {
        if (!recipients.isEmpty()) {
            if (censorableMessage.isAsynchronous()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        sendCensorNotification(recipients, censorableMessage.getPlayer());
                    }
                }.runTask(notCensor);
            } else {
                sendCensorNotification(recipients, censorableMessage.getPlayer());
            }
        }
    }

    private static void sendCensorNotification(Set<Player> recipients, Player sender) {
        for (Player player : recipients) {
            if (player.hasPermission(NotCensor.ADMIN_PERMISSION) && !player.equals(sender))
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', censorNotificationMessage));
        }
    }

    /**
     * Checks the message for each censored word and replaces it with a censored version
     * @param message Message to be checked
     * @return A censored message
     */
    private static String starMessage(String message) {
        String lowercaseMessage = message.toLowerCase(Locale.ROOT);
        for (String word : censoredWords) {
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
    private static String starWord(String word) {
        double censorPercent = (double) replacementPercent / 100;
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

    private static String starCenter(String word, double censorPercent) {
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



    private static void removeMessage(CensorableMessage censorableMessage, boolean duplicate) {
        Set<Player> recipientsCopy = new HashSet<>(censorableMessage.getRecipients());
        boolean anyRecipientsRemoved;
        if (censorableMessage.isBroadcast()) {
            // there could be undisclosed recipients receiving this message
            anyRecipientsRemoved = true;
            forceSendUncensoredMessage(censorableMessage); // this cancels the event
        } else {
            try {
                // modify recipients to be only those with a view permission
                anyRecipientsRemoved = censorableMessage.getRecipients().removeIf(player -> !player.hasPermission(NotCensor.VIEW_PERMISSION) && !player.equals(censorableMessage.getPlayer()));
            } catch (UnsupportedOperationException e) {
                anyRecipientsRemoved = forceSendUncensoredMessage(censorableMessage);
            }
        }

        if (!duplicate && censorNotificationEnabled && anyRecipientsRemoved) {
            // notify admins
            if (censorableMessage.isAsynchronous()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        sendRemovedMessageNotification(censorableMessage, recipientsCopy);
                    }
                }.runTask(notCensor);
            } else {
                sendRemovedMessageNotification(censorableMessage, recipientsCopy);
            }
        }
    }

    private static void sendRemovedMessageNotification(CensorableMessage censorableMessage, Set<Player> originalRecipients) {
        // send removed message notification to admins in the original recipients
        String colorChangedMessage = ChatColor.GRAY + "" + ChatColor.ITALIC + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', censorableMessage.getDisplayName() + " > " + censorableMessage.getMessage()));
        for (Player player : originalRecipients) {
            if (player.hasPermission(NotCensor.ADMIN_PERMISSION) && !player.equals(censorableMessage.getPlayer())) {
                // player is an admin
                // send notification message
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', censorNotificationMessage));
                if (!player.hasPermission(NotCensor.VIEW_PERMISSION)) {
                    // send color changed message
                    player.sendMessage(colorChangedMessage);
                }
            }
        }
    }

    /**
     * Forcibly send an uncensored message to any recipients that have permission to see the message.
     * This method also cancels the message for other players.
     * @param censorableMessage Message to send.
     * @return True if any recipients of the original message did not receive an uncensored message
     */
    private static boolean forceSendUncensoredMessage(CensorableMessage censorableMessage) {
        // can't remove recipients from this set
        censorableMessage.setCancelled(true);

        // construct a new modifiable set with only the players that need to be sent a message
        Set<Player> uncensoredRecipients = new HashSet<>(censorableMessage.getRecipients());
        boolean anyRecipientsRemoved = uncensoredRecipients.removeIf(player -> !player.hasPermission(NotCensor.VIEW_PERMISSION) && !player.equals(censorableMessage.getPlayer()));
        // send message to desired players
        if (!uncensoredRecipients.isEmpty()) {
            if (censorableMessage.isAsynchronous()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        censorableMessage.sendPseudoMessage(censorableMessage.getMessage(), uncensoredRecipients);
                    }
                }.runTask(notCensor);
            } else {
                censorableMessage.sendPseudoMessage(censorableMessage.getMessage(), uncensoredRecipients);
            }
        }

        return anyRecipientsRemoved;
    }

    private static boolean shouldMessageBeCensored(String text) {
        text = text.toLowerCase(Locale.ROOT);
        for (String word : censoredWords)
            if (text.equals(word) || text.startsWith(word + " ") || text.endsWith(" " + word) || text.contains(" " + word + " "))
                return true;
        return false;
    }
}
