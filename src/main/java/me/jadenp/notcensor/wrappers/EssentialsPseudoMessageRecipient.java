package me.jadenp.notcensor.wrappers;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.messaging.IMessageRecipient;
import com.earth2me.essentials.messaging.SimpleMessageRecipient;
import net.ess3.api.IUser;

/**
 * This code is taken directly from the EssentialsX GitHub <a href="https://github.com/EssentialsX/Essentials/blob/2.x/Essentials/src/main/java/com/earth2me/essentials/messaging/SimpleMessageRecipient.java#L192">SimpleMessageRecipient</a>.
 */
public class EssentialsPseudoMessageRecipient extends SimpleMessageRecipient {
    private final IMessageRecipient parent;

    public EssentialsPseudoMessageRecipient(IEssentials ess, IMessageRecipient parent) {
        super(ess, parent);
        this.parent = parent;
    }

    /**
     * Code was removed to stop the recipient from getting anything, but returning an accurate {@link MessageResponse}
     *
     */
    @Override
    public MessageResponse onReceiveMessage(final IMessageRecipient sender, final String message) {
        if (!isReachable()) {
            return MessageResponse.UNREACHABLE;
        }

        final User user = getUser(this);
        boolean afk = false;
        if (user != null) {
            if (user.isIgnoreMsg() && sender instanceof IUser && !((IUser) sender).isAuthorized("essentials.msgtoggle.bypass")) { // Don't ignore console and senders with permission
                return MessageResponse.MESSAGES_IGNORED;
            }
            afk = user.isAfk();
            // Check whether this recipient ignores the sender, only if the sender is not the console.
            if (sender instanceof IUser && user.isIgnoredPlayer((IUser) sender)) {
                return MessageResponse.SENDER_IGNORED;
            }
        }
        // Originally, the message is sent to display to the receiver and the reply recipient is set

        return afk ? MessageResponse.SUCCESS_BUT_AFK : MessageResponse.SUCCESS;
    }

    /**
     * Code was removed to stop the sender from getting anything, but triggering {@link #onReceiveMessage(IMessageRecipient, String)}
     *
     */
    @Override
    public MessageResponse sendMessage(final IMessageRecipient recipient, String message) {

        return recipient.onReceiveMessage(this.parent, message);
    }
}
