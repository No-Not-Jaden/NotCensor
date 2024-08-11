package me.jadenp.notcensor.wrappers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DiscordToMinecraftMessage implements CensorableMessage{

    private final DiscordGuildMessagePostProcessEvent event;
    private String originalMessage;
    private Player linkedPlayer = null;

    public DiscordToMinecraftMessage(DiscordGuildMessagePostProcessEvent event) {
        this.event = event;

        this.originalMessage = MessageUtil.toPlain(event.getMinecraftMessage(), false);
        UUID linkedUUID = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getAuthor().getId());
        if (linkedUUID != null)
            linkedPlayer = Bukkit.getPlayer(linkedUUID);
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
        Set<Player> recipient = new HashSet<>();
        if (linkedPlayer != null)
            recipient.add(linkedPlayer);
        return Collections.unmodifiableSet(recipient);
    }

    @Override
    public String getMessage() {
        return originalMessage;
    }

    @Override
    public void sendPseudoMessage(String message, Set<Player> recipients) {
        if (recipients.isEmpty()) {
            // recipients will be empty if the message should be changed to be censored

            event.setMinecraftMessage(MessageUtil.toComponent(message));
            this.originalMessage = message;
        } else {
            // message should be sent manually
            for (Player player : recipients) {
                MessageUtil.sendMessage(player, MessageUtil.reserializeToMinecraft(message));
            }
        }
    }

    @Override
    public Player getPlayer() {
        return linkedPlayer;
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
        return event.getAuthor().getDisplayName();
    }

    @Override
    public String getID() {
        return event.getAuthor().getId();
    }

    @Override
    public boolean isChatMonitor() {
        return false;
    }
}
