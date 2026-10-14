package me.plugin.scrates;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class HologramEditManager {

    private sCrates plugin;
    private Map<String, Integer> selectedLines;

    public HologramEditManager(sCrates plugin) {
        this.plugin = plugin;
        this.selectedLines = new HashMap<>();
    }

    public int getSelectedLine(Player player, String crateName) {
        String key = player.getUniqueId().toString() + ":" + crateName.toLowerCase();
        return selectedLines.getOrDefault(key, 0);
    }

    public void setSelectedLine(Player player, String crateName, int line) {
        String key = player.getUniqueId().toString() + ":" + crateName.toLowerCase();
        selectedLines.put(key, line);
    }

    public void removePlayer(Player player) {
        String prefix = player.getUniqueId().toString() + ":";
        selectedLines.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
