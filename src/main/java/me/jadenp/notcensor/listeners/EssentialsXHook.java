package me.jadenp.notcensor.listeners;

import com.earth2me.essentials.Essentials;
import me.jadenp.notcensor.CensorManager;
import me.jadenp.notcensor.wrappers.EssentialsPrivateMessage;
import net.ess3.api.events.PrivateMessagePreSendEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class EssentialsXHook implements Listener {

    private final Essentials essentials;
    private static boolean editPrivateMessages = false;

    public EssentialsXHook() {
        essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
    }

    public static void setEditPrivateMessages(boolean editPrivateMessages) {
        EssentialsXHook.editPrivateMessages = editPrivateMessages;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPrivateMessagePreSend(PrivateMessagePreSendEvent event) {
        if (!editPrivateMessages)
            return;
        Player sender = essentials.getUser(event.getSender().getUUID()).getBase();
        Player receiver = essentials.getUser(event.getRecipient().getUUID()).getBase();

        if (sender != null && receiver != null && !sender.equals(receiver))
            CensorManager.processMessage(new EssentialsPrivateMessage(event, sender, receiver, essentials));
    }



}
