package me.jadenp.notcensor.wrappers;

import github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MinecraftToDiscordMessage implements CensorableMessage{
    private final GameChatMessagePostProcessEvent event;

    public MinecraftToDiscordMessage(GameChatMessagePostProcessEvent event) {
        this.event = event;
    }

    @Override
    public Set<Player> getRecipients() {
        Set<Player> recipient = new HashSet<>();
        recipient.add(event.getPlayer());
        return Collections.unmodifiableSet(recipient);
    }

    @Override
    public String getMessage() {
        return event.getProcessedMessage();
    }

    @Override
    public void sendPseudoMessage(String message, Set<Player> recipients) {
        // recipients will be empty if the message needs to be changed to a censored version
        if (recipients.isEmpty()) {
            event.setProcessedMessage(message);
        }
    }

    @Override
    public Player getPlayer() {
        return event.getPlayer();
    }

    @Override
    public boolean isBroadcast() {
        return true;
    }

    @Override
    public boolean isAsynchronous() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return event.getPlayer().getDisplayName();
    }

    @Override
    public String getID() {
        return "MC->DC" + event.getPlayer().getUniqueId();
    }

    @Override
    public boolean isChatMonitor() {
        return true;
    }

    @Override
    public boolean isCancelled() {
        return event.isCancelled();
    }

    @Override
    public void setCancelled(boolean b) {
        event.setCancelled(b);
    }
}
