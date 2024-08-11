package me.jadenp.notcensor.listeners;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent;
import me.jadenp.notcensor.CensorManager;
import me.jadenp.notcensor.wrappers.DiscordToMinecraftMessage;
import me.jadenp.notcensor.wrappers.MinecraftToDiscordMessage;

public class DiscordSRVHook {

    private static boolean editMessages = false;

    public static void setEditMessages(boolean editMessages) {
        DiscordSRVHook.editMessages = editMessages;
    }

    @Subscribe
    public void onDiscordToMinecraft(DiscordGuildMessagePostProcessEvent event) {
        if (editMessages)
            CensorManager.processMessage(new DiscordToMinecraftMessage(event));
    }

    @Subscribe
    public void onMinecraftToDiscord(GameChatMessagePostProcessEvent event) {
        if (editMessages)
            CensorManager.processMessage(new MinecraftToDiscordMessage(event));
    }

}
