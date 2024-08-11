package me.jadenp.notcensor.wrappers;

import com.earth2me.essentials.Essentials;
import net.ess3.api.events.PrivateMessagePreSendEvent;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EssentialsPrivateMessage implements CensorableMessage{

    private final PrivateMessagePreSendEvent event;
    private final Set<Player> recipients;
    private final Player sender;
    private final Essentials essentials;

    public EssentialsPrivateMessage(PrivateMessagePreSendEvent event, Player sender, Player receiver, Essentials essentials) {
        this.event = event;
        Set<Player> mutableSet = new HashSet<>();
        mutableSet.add(sender);
        mutableSet.add(receiver);
        recipients = Collections.unmodifiableSet(mutableSet);
        this.sender = sender;
        this.essentials = essentials;
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
        return recipients;
    }

    @Override
    public String getMessage() {
        return event.getMessage();
    }

    @Override
    public void sendPseudoMessage(String message, Set<Player> recipients) {
        for (Player player : recipients) {
            // There should only be 2 people to send the pseudo message to
            if (player.getUniqueId().equals(event.getSender().getUUID())) {
                // sender receives a message, but receiver doesn't
                event.getSender().sendMessage(new EssentialsPseudoMessageRecipient(essentials, event.getRecipient()), message);
            } else if (player.getUniqueId().equals(event.getRecipient().getUUID())){
                // sender doesn't receive a message, but receiver does
                new EssentialsPseudoMessageRecipient(essentials, event.getSender()).sendMessage(event.getRecipient(), message);
            }
        }
    }

    @Override
    public Player getPlayer() {
        return sender;
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
        return event.getSender().getDisplayName();
    }

    @Override
    public String getID() {
        return event.getSender().getUUID().toString();
    }

    @Override
    public boolean isChatMonitor() {
        // returning true here will ignore the message if the exact message was just sent
        // calling a pseudo message for the sender will fire the essentials private message event again
        return event.getSender() instanceof EssentialsPseudoMessageRecipient || event.getRecipient() instanceof EssentialsPseudoMessageRecipient;
    }
}
