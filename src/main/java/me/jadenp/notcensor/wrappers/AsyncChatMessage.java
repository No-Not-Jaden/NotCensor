package me.jadenp.notcensor.wrappers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;

public class AsyncChatMessage implements CensorableMessage{

    private final AsyncPlayerChatEvent event;

    public AsyncChatMessage(AsyncPlayerChatEvent event) {
        this.event = event;
    }

    @Override
    public boolean isCancelled() {
        return event.isCancelled();
    }

    @Override
    public void setCancelled(boolean cancel) {
        event.setCancelled(cancel);
    }

    @Override
    public Set<Player> getRecipients() {
        return event.getRecipients();
    }

    @Override
    public String getMessage() {
        return event.getMessage();
    }

    @Override
    public Player getPlayer() {
        return event.getPlayer();
    }

    @Override
    public boolean isBroadcast() {
        return false;
    }

    @Override
    public boolean isAsynchronous() {
        return event.isAsynchronous();
    }

    @Override
    public String getDisplayName() {
        return event.getPlayer().getDisplayName();
    }

    @Override
    public String getID() {
        return event.getPlayer().getUniqueId().toString();
    }

    @Override
    public boolean isChatMonitor() {
        return false;
    }

    @Override
    public void sendPseudoMessage(String message, Set<Player> recipients) {
        String formattedMessage = getFormattedPlayerMessage(event.getPlayer(), message, recipients);
        recipients.forEach(player -> player.sendMessage(formattedMessage));
    }

    private String getFormattedPlayerMessage(Player player, String messageReplacement, Set<Player> recipients) {
        // call AsyncPlayerChatEvent manually to allow other plugins to modify the message
        AsyncPlayerChatEvent newMessage = new AsyncPlayerChatEvent(event.isAsynchronous(), player, messageReplacement, recipients);
        Bukkit.getPluginManager().callEvent(newMessage);
        return String.format(newMessage.getFormat(), player.getDisplayName(), newMessage.getMessage());
    }
}
