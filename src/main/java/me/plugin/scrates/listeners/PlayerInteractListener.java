package me.plugin.scrates.listeners;

import me.plugin.scrates.Crate;
import me.plugin.scrates.KeyEditManager;
import me.plugin.scrates.RewardData;
import me.plugin.scrates.sCrates;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayerInteractListener implements Listener {

    private sCrates plugin;
    private Random random = new Random();

    public PlayerInteractListener(sCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (plugin.getKeyEditManager().isEditingKey(player)) {
            handleKeyEditInteract(event, player);
            return;
        }

        Action action = event.getAction();
        ItemStack heldItem = player.getItemInHand();

        // Check if the held item is a crate key
        Crate keyCrate = null;
        if (heldItem != null && heldItem.getType() != Material.AIR) {
            keyCrate = findCrateByKey(heldItem);
        }

        // Try to find crate by clicked block
        Crate blockCrate = null;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            blockCrate = plugin.getCrateManager().getCrateByLocation(clickedBlock.getLocation());
        }

        // Determine which crate we're interacting with
        Crate targetCrate = null;
        if (keyCrate != null) {
            targetCrate = keyCrate;
        } else if (blockCrate != null) {
            targetCrate = blockCrate;
        }

        if (targetCrate == null) return;

        if (targetCrate.isVirtualMode()) {
            // Virtual Mode ON
            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                plugin.getMenuManager().openPreviewMenu(player, targetCrate);
            } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                // Right-click opens/activates the crate - consume 1 key and deliver reward
                if (keyCrate != null && heldItem != null) {
                    rollAndDeliverReward(player, targetCrate, heldItem);
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou need a key to open this crate."));
                }
            }
        } else {
            // Virtual Mode OFF
            if (action == Action.LEFT_CLICK_BLOCK && blockCrate != null) {
                event.setCancelled(true);
                plugin.getMenuManager().openPreviewMenu(player, blockCrate);
            } else if (action == Action.RIGHT_CLICK_BLOCK && blockCrate != null) {
                event.setCancelled(true);
                if (keyCrate != null && heldItem != null) {
                    rollAndDeliverReward(player, blockCrate, heldItem);
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou need a key to open this crate."));
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Creative mode block-break protection for crate blocks
        Block block = event.getBlock();
        Crate crate = plugin.getCrateManager().getCrateByLocation(block.getLocation());
        if (crate != null) {
            event.setCancelled(true);
        }
    }

    private void handleKeyEditInteract(PlayerInteractEvent event, Player player) {
        KeyEditManager.KeyEditState state = plugin.getKeyEditManager().getState(player);
        if (state == null) return;

        Action action = event.getAction();
        String crateName = state.crateName;
        Crate crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null) {
            plugin.getKeyEditManager().stopKeyEdit(player);
            return;
        }

        switch (state.step) {
            case WAITING_LEFT_CLICK:
                if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    player.getInventory().setItem(3, null);
                    player.updateInventory();
                    plugin.getKeyEditManager().setStep(player, KeyEditManager.KeyEditStep.WAITING_SHIFT_RIGHT_CLICK);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aShift + Right Click to set a new crate item"));
                }
                break;

            case WAITING_SHIFT_RIGHT_CLICK:
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    if (player.isSneaking()) {
                        event.setCancelled(true);
                        ItemStack heldItem = player.getItemInHand();
                        if (heldItem != null && heldItem.getType() != Material.AIR) {
                            int heldSlot = player.getInventory().getHeldItemSlot();
                            if (heldSlot == 3) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCannot use the key slot item. Hold a different item."));
                                return;
                            }

                            ItemStack newKeyItem = heldItem.clone();
                            newKeyItem.setAmount(1);

                            ItemStack prevKey = crate.getKeyItem();
                            if (prevKey != null && prevKey.hasItemMeta()) {
                                ItemMeta prevMeta = prevKey.getItemMeta();
                                ItemMeta newMeta = newKeyItem.getItemMeta();
                                if (newMeta != null) {
                                    if (prevMeta.hasDisplayName()) {
                                        newMeta.setDisplayName(prevMeta.getDisplayName());
                                    }
                                    if (prevMeta.hasLore()) {
                                        newMeta.setLore(new ArrayList<>(prevMeta.getLore()));
                                    }
                                    newKeyItem.setItemMeta(newMeta);
                                }
                            }

                            player.getInventory().setItem(heldSlot, null);
                            player.getInventory().setItem(3, newKeyItem);
                            player.updateInventory();

                            crate.setKeyItem(newKeyItem);
                            plugin.getCrateManager().saveCrate(crate);

                            plugin.getKeyEditManager().setStep(player, KeyEditManager.KeyEditStep.WAITING_RIGHT_CLICK_SAVE);
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aRight Click to save the new key item."));
                        }
                    }
                }
                break;

            case WAITING_RIGHT_CLICK_SAVE:
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    player.getInventory().setItem(3, null);
                    player.updateInventory();

                    plugin.getKeyEditManager().stopKeyEdit(player);
                    plugin.getMenuManager().openCrateEditorMenu(player, crate);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aKey item has been updated successfully!"));
                }
                break;
        }
    }

    private Crate findCrateByKey(ItemStack heldItem) {
        if (heldItem == null || heldItem.getType() == Material.AIR) return null;
        for (Crate crate : plugin.getCrateManager().getCrates().values()) {
            ItemStack keyItem = crate.getKeyItem();
            if (keyItem != null && keyItem.isSimilar(heldItem)) {
                return crate;
            }
        }
        return null;
    }

    private void rollAndDeliverReward(Player player, Crate crate, ItemStack keyItem) {
        List<ItemStack> previewItems = crate.getPreviewItems();
        if (previewItems == null || previewItems.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cThis crate has no rewards configured."));
            return;
        }

        // FIX 1: Build list of only non-empty slots with actual rewards
        List<ItemStack> validItems = new ArrayList<>();
        List<Double> chances = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();

        for (int i = 0; i < previewItems.size(); i++) {
            ItemStack item = previewItems.get(i);
            if (item != null && item.getType() != Material.AIR) {
                RewardData rd = crate.getRewardData(i);
                validItems.add(item);
                chances.add(rd.getChance());
                slots.add(i);
            }
        }

        if (validItems.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cThis crate has no rewards configured."));
            return;
        }

        // Weighted random selection - only from non-empty slots
        double totalWeight = 0;
        for (double chance : chances) {
            totalWeight += chance;
        }

        if (totalWeight <= 0) {
            // All chances are 0 - pick uniformly at random so player always gets something
            int randomIndex = random.nextInt(validItems.size());
            int previewSlot = slots.get(randomIndex);
            ItemStack rewardItem = validItems.get(randomIndex);
            RewardData rd = crate.getRewardData(previewSlot);

            consumeKey(player, keyItem);
            deliverReward(player, crate, rewardItem, rd);
            return;
        }

        double roll = random.nextDouble() * totalWeight;
        int selectedIndex = 0;
        double cumulative = 0;
        for (int i = 0; i < chances.size(); i++) {
            cumulative += chances.get(i);
            if (roll < cumulative) {
                selectedIndex = i;
                break;
            }
        }

        int previewSlot = slots.get(selectedIndex);
        ItemStack rewardItem = validItems.get(selectedIndex);
        RewardData rd = crate.getRewardData(previewSlot);

        consumeKey(player, keyItem);
        deliverReward(player, crate, rewardItem, rd);
    }

    private void consumeKey(Player player, ItemStack keyItem) {
        int newAmount = keyItem.getAmount() - 1;
        if (newAmount <= 0) {
            player.setItemInHand(null);
        } else {
            keyItem.setAmount(newAmount);
            player.setItemInHand(keyItem);
        }
        player.updateInventory();
    }

    private void deliverReward(Player player, Crate crate, ItemStack rewardItem, RewardData rd) {
        if (rd.isGiveItem()) {
            ItemStack reward = rewardItem.clone();
            plugin.getMenuManager().stripPreviewLore(reward);
            player.getInventory().addItem(reward);
        }

        List<String> commands = rd.getCommands();
        for (String cmd : commands) {
            String resolved = cmd
                    .replace("%player%", player.getName())
                    .replace("%playerId%", player.getUniqueId().toString())
                    .replace("%amount%", String.valueOf(rewardItem.getAmount()))
                    .replace("%crate%", crate.getDisplayNameColored());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aYou opened a crate and received a reward!"));
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.getKeyEditManager().isEditingKey(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getChatInputManager().removePendingInput(player);
        plugin.getMenuManager().removeOpenMenu(player);
        plugin.getHologramEditManager().removePlayer(player);
        plugin.getRewardEditManager().removePlayer(player);
        plugin.getAnimationEditManager().removePlayer(player);

        if (plugin.getKeyEditManager().isEditingKey(player)) {
            player.getInventory().setItem(3, null);
            plugin.getKeyEditManager().stopKeyEdit(player);
        }
    }
}
