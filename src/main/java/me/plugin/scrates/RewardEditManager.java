package me.plugin.scrates;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class RewardEditManager {

    private sCrates plugin;
    private Map<String, EditingState> states;

    public RewardEditManager(sCrates plugin) {
        this.plugin = plugin;
        this.states = new HashMap<>();
    }

    public void startEdit(Player player, String crateName, int previewSlot) {
        String key = player.getUniqueId().toString();
        states.put(key, new EditingState(crateName, previewSlot, 0));
    }

    public EditingState getEditState(Player player) {
        return states.get(player.getUniqueId().toString());
    }

    public boolean isEditingReward(Player player) {
        return states.containsKey(player.getUniqueId().toString());
    }

    public void stopEdit(Player player) {
        states.remove(player.getUniqueId().toString());
    }

    public void setSelectedCommand(Player player, int index) {
        EditingState state = states.get(player.getUniqueId().toString());
        if (state != null) {
            state.selectedCommand = index;
        }
    }

    public void removePlayer(Player player) {
        states.remove(player.getUniqueId().toString());
    }

    public static class EditingState {
        public String crateName;
        public int previewSlot;
        public int selectedCommand;

        public EditingState(String crateName, int previewSlot, int selectedCommand) {
            this.crateName = crateName;
            this.previewSlot = previewSlot;
            this.selectedCommand = selectedCommand;
        }
    }
}
