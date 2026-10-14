package me.plugin.scrates;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ChatInputManager {

    private Map<String, PendingInput> pendingInputs;

    public ChatInputManager() {
        this.pendingInputs = new HashMap<>();
    }

    public void setPendingInput(Player player, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId().toString(), new PendingInput(callback));
    }

    public void setPendingInput(Player player, Consumer<String> callback, Runnable onCancel) {
        pendingInputs.put(player.getUniqueId().toString(), new PendingInput(callback, onCancel));
    }

    public boolean hasPendingInput(Player player) {
        return pendingInputs.containsKey(player.getUniqueId().toString());
    }

    public PendingInput getPendingInput(Player player) {
        return pendingInputs.get(player.getUniqueId().toString());
    }

    public void removePendingInput(Player player) {
        pendingInputs.remove(player.getUniqueId().toString());
    }

    public void clearInput(Player player) {
        pendingInputs.remove(player.getUniqueId().toString());
    }

    public static class PendingInput {
        private Consumer<String> callback;
        private Runnable onCancel;

        public PendingInput(Consumer<String> callback) {
            this.callback = callback;
            this.onCancel = null;
        }

        public PendingInput(Consumer<String> callback, Runnable onCancel) {
            this.callback = callback;
            this.onCancel = onCancel;
        }

        public Consumer<String> getCallback() {
            return callback;
        }

        public Runnable getOnCancel() {
            return onCancel;
        }

        public boolean hasCancelHandler() {
            return onCancel != null;
        }
    }
}
