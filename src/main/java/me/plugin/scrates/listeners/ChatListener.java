package me.plugin.scrates.listeners;

import me.plugin.scrates.ChatInputManager;
import me.plugin.scrates.sCrates;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private sCrates plugin;

    public ChatListener(sCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getChatInputManager().hasPendingInput(player)) {
            return;
        }

        // Cancel the chat event so the message doesn't appear publicly
        event.setCancelled(true);

        String message = event.getMessage();
        ChatInputManager.PendingInput pending = plugin.getChatInputManager().getPendingInput(player);
        plugin.getChatInputManager().removePendingInput(player);

        // Handle cancel
        if (message.equalsIgnoreCase("cancel")) {
            if (pending.hasCancelHandler()) {
                // Run cancel handler on the main thread
                final Runnable cancelHandler = pending.getOnCancel();
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aAction cancelled."));
                    cancelHandler.run();
                });
            } else {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aAction cancelled."));
                });
            }
            return;
        }

        // Process the input on the main thread (since chat event is async)
        final String input = message;
        final java.util.function.Consumer<String> callback = pending.getCallback();
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            callback.accept(input);
        });
    }
}
