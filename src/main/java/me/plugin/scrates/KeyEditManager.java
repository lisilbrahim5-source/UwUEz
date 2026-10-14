package me.plugin.scrates;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class KeyEditManager {

    private sCrates plugin;
    private Map<String, KeyEditState> states;

    public KeyEditManager(sCrates plugin) {
        this.plugin = plugin;
        this.states = new HashMap<>();
    }

    public void startKeyEdit(Player player, String crateName) {
        states.put(player.getUniqueId().toString(), new KeyEditState(crateName, KeyEditStep.WAITING_LEFT_CLICK));
    }

    public boolean isEditingKey(Player player) {
        return states.containsKey(player.getUniqueId().toString());
    }

    public KeyEditState getState(Player player) {
        return states.get(player.getUniqueId().toString());
    }

    public String getEditingCrateName(Player player) {
        KeyEditState state = states.get(player.getUniqueId().toString());
        return state != null ? state.crateName : null;
    }

    public void setStep(Player player, KeyEditStep step) {
        KeyEditState state = states.get(player.getUniqueId().toString());
        if (state != null) {
            state.step = step;
        }
    }

    public void stopKeyEdit(Player player) {
        states.remove(player.getUniqueId().toString());
    }

    public enum KeyEditStep {
        WAITING_LEFT_CLICK,
        WAITING_SHIFT_RIGHT_CLICK,
        WAITING_RIGHT_CLICK_SAVE
    }

    public static class KeyEditState {
        public String crateName;
        public KeyEditStep step;

        public KeyEditState(String crateName, KeyEditStep step) {
            this.crateName = crateName;
            this.step = step;
        }
    }
}
