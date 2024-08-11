package me.jadenp.notcensor.listeners;

import me.jadenp.notcensor.CensorManager;
import me.jadenp.notcensor.wrappers.AsyncChatMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        CensorManager.processMessage(new AsyncChatMessage(event));
    }

}
