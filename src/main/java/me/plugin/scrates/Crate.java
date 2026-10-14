package me.plugin.scrates;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Crate {

    private String name;
    private String displayName;
    private int previewRows;
    private Location blockLocation;
    private ItemStack keyItem;
    private List<ItemStack> previewItems;
    private List<String> hologramLines;
    private boolean virtualMode;
    private double hologramHeightOffset;
    private Map<Integer, RewardData> rewardData;
    private AnimationData animationData;

    public Crate(String name) {
        this.name = name;
        this.displayName = name;
        this.previewRows = 6;
        this.blockLocation = null;
        this.previewItems = new ArrayList<>();
        this.keyItem = createDefaultKeyItem(name);
        this.hologramLines = new ArrayList<>();
        this.hologramLines.add("%displayName%");
        this.hologramLines.add("&e&l&ndiscord.gg/G9etM7zrJM");
        this.virtualMode = true;
        this.hologramHeightOffset = 0.0;
        this.rewardData = new HashMap<>();
        this.animationData = new AnimationData();
    }

    private ItemStack createDefaultKeyItem(String crateName) {
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = key.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7This is a Example Lore"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7You can edit it yourself"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Suggestions? &a&lhttps://discord.gg/G9etM7zrJM"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Owner: &c&lbrygnuaip"));
        meta.setLore(lore);

        key.setItemMeta(meta);
        return key;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayNameColored() {
        return ChatColor.translateAlternateColorCodes('&', displayName);
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        if (keyItem != null && keyItem.hasItemMeta()) {
            ItemMeta meta = keyItem.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            keyItem.setItemMeta(meta);
        }
    }

    public int getPreviewRows() {
        return previewRows;
    }

    public void setPreviewRows(int previewRows) {
        this.previewRows = previewRows;
    }

    public Location getBlockLocation() {
        return blockLocation;
    }

    public void setBlockLocation(Location blockLocation) {
        this.blockLocation = blockLocation;
    }

    public ItemStack getKeyItem() {
        return keyItem;
    }

    public void setKeyItem(ItemStack keyItem) {
        this.keyItem = keyItem;
    }

    public List<ItemStack> getPreviewItems() {
        return previewItems;
    }

    public void setPreviewItems(List<ItemStack> previewItems) {
        this.previewItems = previewItems;
    }

    public List<String> getHologramLines() {
        return hologramLines;
    }

    public void setHologramLines(List<String> hologramLines) {
        this.hologramLines = hologramLines != null ? hologramLines : new ArrayList<>();
    }

    public boolean isVirtualMode() {
        return virtualMode;
    }

    public void setVirtualMode(boolean virtualMode) {
        this.virtualMode = virtualMode;
    }

    public double getHologramHeightOffset() {
        return hologramHeightOffset;
    }

    public void setHologramHeightOffset(double hologramHeightOffset) {
        this.hologramHeightOffset = hologramHeightOffset;
    }

    public Map<Integer, RewardData> getRewardData() {
        return rewardData;
    }

    public void setRewardData(Map<Integer, RewardData> rewardData) {
        this.rewardData = rewardData != null ? rewardData : new HashMap<>();
    }

    public RewardData getRewardData(int slot) {
        return rewardData.computeIfAbsent(slot, k -> new RewardData());
    }

    public String resolveHologramLine(String line) {
        String resolved = line.replace("%displayName%", getDisplayNameColored());
        return ChatColor.translateAlternateColorCodes('&', resolved);
    }

    public int getMenuSize() {
        return (1 + previewRows) * 9;
    }

    public int getPreviewMenuSize() {
        return previewRows * 9;
    }

    public AnimationData getAnimationData() {
        return animationData;
    }

    public void setAnimationData(AnimationData animationData) {
        this.animationData = animationData != null ? animationData : new AnimationData();
    }
}
