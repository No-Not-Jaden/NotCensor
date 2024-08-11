package me.jadenp.notcensor.wrappers;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Represents a message that can be censored.
 */
public interface CensorableMessage extends Cancellable {

    /**
     * Get the recipients of the message.
     * A modifiable set should be returned if modifying this set will modify which players receive the message.
     * An unmodifiable set should be returned if modifying this set won't modify the outcome of the message.
     * @return A set of players intended to receive the message.
     */
    Set<Player> getRecipients();

    /**
     * Get the main message that will be sent and may need censoring.
     * @return The message contents.
     */
    String getMessage();

    /**
     * Send a pseudo message to recipients.
     * This message should look as if the original sender of the message had sent it.
     * @see this.getPlayer()
     * @param message Message to be sent.
     * @param recipients Recipients to receive the message
     */
    void sendPseudoMessage(String message, Set<Player> recipients);

    /**
     * Get the sender of this censorable message
     * @return The player who sent the message.
     */
    @Nullable
    Player getPlayer();

    /**
     * Check if this message is a broadcast and will be sent to undisclosed recipients.
     * If this is true, this.sendPseudoMessage() should modify the broadcast when an empty set is passed as the recipients' parameter.
     * @return Whether the censorable message is a broadcast or not
     */
    boolean isBroadcast();

    /**
     * Check if the message is being sent asynchronously
     * @return True if the message is async
     */
    boolean isAsynchronous();

    /**
     * Get the display name of the sender.
     * This method is required because the returned object of {@link #getPlayer()} may be null.
     * @return The display name of the sender
     */
    String getDisplayName();

    /**
     * Get a unique id for the sender and the message destination.
     * For example, if the message source is a {@link Player}, and the destination is outside of Minecraft chat, append a source-specific string to the player's UUID.
     * This method is required because the returned object of {@link #getPlayer()} may be null.
     * @return The id of the sender.
     */
    String getID();

    /**
     * Check if the censorable message is from monitoring chat.
     * More formally, if the message comes from listening to the {@link org.bukkit.event.player.AsyncPlayerChatEvent}
     * @return Whether this message came from another censorable chat message.
     */
    boolean isChatMonitor();
}
