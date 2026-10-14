package me.plugin.scrates;

import java.util.ArrayList;
import java.util.List;

public class RewardData {

    private double chance;
    private boolean giveItem;
    private List<String> commands;

    public RewardData() {
        this.chance = 90.0;
        this.giveItem = true;
        this.commands = new ArrayList<>();
    }

    public RewardData(double chance, boolean giveItem, List<String> commands) {
        this.chance = chance;
        this.giveItem = giveItem;
        this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = chance;
    }

    public boolean isGiveItem() {
        return giveItem;
    }

    public void setGiveItem(boolean giveItem) {
        this.giveItem = giveItem;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
    }
}
