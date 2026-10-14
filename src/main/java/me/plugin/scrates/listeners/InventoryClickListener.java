package me.plugin.scrates.listeners;

import me.plugin.scrates.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InventoryClickListener implements Listener {

    private sCrates plugin;

    public InventoryClickListener(sCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (plugin.getKeyEditManager().isEditingKey(player)) {
            return;
        }

        MenuManager.OpenMenu openMenu = plugin.getMenuManager().getOpenMenu(player);
        if (openMenu == null) return;

        String menuType = openMenu.getType();

        if (menuType.equals("editor")) {
            handleEditorClick(event, player, event.getView().getTopInventory());
        } else if (menuType.equals("crate_editor")) {
            handleCrateEditorClick(event, player, openMenu, event.getView().getTopInventory());
        } else if (menuType.equals("preview")) {
            event.setCancelled(true);
        } else if (menuType.equals("reward_editor")) {
            handleRewardEditorClick(event, player, openMenu, event.getView().getTopInventory());
        } else if (menuType.equals("pick_color")) {
            handlePickColorClick(event, player, openMenu, event.getView().getTopInventory());
        }
    }

    private void handleEditorClick(InventoryClickEvent event, Player player, Inventory topInv) {
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInv)) {
            return;
        }

        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        if (slot == 4 && clicked.getType() == Material.NETHER_STAR) {
            player.closeInventory();
            plugin.getMenuManager().removeOpenMenu(player);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPlease type crate name or type &ccancel &ato cancel"));

            plugin.getChatInputManager().setPendingInput(player, (input) -> {
                if (plugin.getCrateManager().crateExists(input)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7A crate with that name already exists."));
                    plugin.getMenuManager().openEditorMenu(player);
                    return;
                }
                Crate crate = plugin.getCrateManager().createCrate(input);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aCrate '&f" + crate.getName() + "&a' has been created!"));
                plugin.getMenuManager().openEditorMenu(player);
            }, () -> {
                plugin.getMenuManager().openEditorMenu(player);
            });
            return;
        }

        int[] crateSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        for (int crateSlot : crateSlots) {
            if (slot == crateSlot) {
                List<Crate> crates = new ArrayList<>(plugin.getCrateManager().getCrates().values());
                int index = -1;
                for (int i = 0; i < crateSlots.length; i++) {
                    if (crateSlots[i] == slot) {
                        index = i;
                        break;
                    }
                }
                if (index >= 0 && index < crates.size()) {
                    Crate crate = crates.get(index);
                    plugin.getMenuManager().openCrateEditorMenu(player, crate);
                }
                return;
            }
        }
    }

    private void handleCrateEditorClick(InventoryClickEvent event, Player player, MenuManager.OpenMenu openMenu, Inventory topInv) {
        String crateName = openMenu.getCrateName();
        Crate crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null) {
            event.setCancelled(true);
            return;
        }

        int slot = event.getRawSlot();
        ClickType clickType = event.getClick();

        // Handle Q (drop key) for hologram item in slot 7
        if (clickType == ClickType.DROP && slot == 7) {
            event.setCancelled(true);
            handleHologramHeightEdit(player, crate);
            return;
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInv)) {
            if (slot < 9) {
                event.setCancelled(true);
                handleEditingRowClick(event, player, crate, slot, topInv);
                return;
            }

            boolean editing = plugin.getMenuManager().isEditingMode(player, crate.getName());
            if (editing) {
                event.setCancelled(true);
                if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT) {
                    int previewSlot = slot - 9;
                    List<ItemStack> previewItems = crate.getPreviewItems();
                    if (previewSlot >= 0 && previewSlot < previewItems.size() && previewItems.get(previewSlot) != null) {
                        plugin.getMenuManager().openRewardEditorMenu(player, crate, previewSlot);
                    }
                }
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    savePreviewItems(topInv, crate);
                }, 1L);
            }
        } else {
            boolean editing = plugin.getMenuManager().isEditingMode(player, crate.getName());
            if (editing) {
                event.setCancelled(true);
            } else {
                if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        savePreviewItems(topInv, crate);
                    }, 1L);
                }
            }
        }
    }

    private void handleEditingRowClick(InventoryClickEvent event, Player player, Crate crate, int slot, Inventory topInv) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        ClickType clickType = event.getClick();

        switch (slot) {
            case 0:
                crate.setVirtualMode(!crate.isVirtualMode());
                plugin.getCrateManager().saveCrate(crate);
                plugin.getMenuManager().openCrateEditorMenu(player, crate);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aVirtual mode is now " + (crate.isVirtualMode() ? "&aON" : "&cOFF") + "&a."));
                break;

            case 1:
                handleKeyEdit(player, crate);
                break;

            case 2:
                handleRowsEdit(player, crate);
                break;

            case 4:
                plugin.getMenuManager().toggleEditingMode(player, crate.getName());
                plugin.getMenuManager().openCrateEditorMenu(player, crate);
                break;

            case 5:
                plugin.getMenuManager().openPickColorMenu(player, crate);
                break;

            case 6:
                handleDisplayNameEdit(player, crate);
                break;

            case 7:
                handleHologramTextClick(event, player, crate, topInv);
                break;

            case 8:
                handleAnimationVisibleClick(event, player, crate, topInv);
                break;
        }
    }

    private void handleAnimationVisibleClick(InventoryClickEvent event, Player player, Crate crate, Inventory topInv) {
        ClickType clickType = event.getClick();
        List<String> lines = crate.getHologramLines();
        int selectedLine = plugin.getAnimationEditManager().getSelectedLine(player, crate.getName());
        AnimationData data = crate.getAnimationData();

        if (clickType == ClickType.LEFT) {
            if (selectedLine < lines.size() - 1) { selectedLine++; } else { selectedLine = 0; }
            plugin.getAnimationEditManager().setSelectedLine(player, crate.getName(), selectedLine);
            topInv.setItem(8, plugin.getMenuManager().createAnimationVisibleItem(crate, player));
        } else if (clickType == ClickType.RIGHT) {
            if (selectedLine > 0) { selectedLine--; } else { selectedLine = lines.size() - 1; }
            plugin.getAnimationEditManager().setSelectedLine(player, crate.getName(), selectedLine);
            topInv.setItem(8, plugin.getMenuManager().createAnimationVisibleItem(crate, player));
        } else if (clickType == ClickType.SHIFT_LEFT) {
            if (!data.hasColor()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPlease select an animation color first."));
                return;
            }
            data.addLine(selectedLine);
            data.setDisplayNameOnly(false);
            plugin.getCrateManager().saveCrate(crate);
            plugin.getAnimationManager().restartAnimation(crate);
            topInv.setItem(8, plugin.getMenuManager().createAnimationVisibleItem(crate, player));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aAnimation enabled on line " + selectedLine + "!"));
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            if (data.isLineAnimated(selectedLine)) {
                if (plugin.getAnimationManager().isLineAnimating(crate, selectedLine)) {
                    player.closeInventory();
                    plugin.getMenuManager().removeOpenMenu(player);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aWaiting for the animation to finish..."));
                    plugin.getAnimationManager().stopLineAnimation(crate, selectedLine);
                    data.removeLine(selectedLine);
                    plugin.getCrateManager().saveCrate(crate);
                } else {
                    data.removeLine(selectedLine);
                    plugin.getCrateManager().saveCrate(crate);
                    plugin.getAnimationManager().restartAnimation(crate);
                    topInv.setItem(8, plugin.getMenuManager().createAnimationVisibleItem(crate, player));
                }
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cThat line has no animation."));
            }
        } else if (clickType == ClickType.MIDDLE) {
            if (!data.hasColor()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPlease select an animation color first."));
                return;
            }
            data.getLines().clear();
            data.setDisplayNameOnly(true);
            data.setDisplayNameAnimated(true);
            plugin.getCrateManager().saveCrate(crate);
            plugin.getAnimationManager().restartAnimation(crate);
            topInv.setItem(8, plugin.getMenuManager().createAnimationVisibleItem(crate, player));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aAnimation set to DisplayName only mode!"));
        }
    }

    @SuppressWarnings("deprecation")
    private void handlePickColorClick(InventoryClickEvent event, Player player, MenuManager.OpenMenu openMenu, Inventory topInv) {
        String crateName = openMenu.getCrateName();
        Crate crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInv)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 3) {
            plugin.getMenuManager().openCrateEditorMenu(player, crate);
            return;
        }

        if (slot == 29) {
            AnimationData data = crate.getAnimationData();
            data.reset();
            plugin.getCrateManager().saveCrate(crate);
            plugin.getAnimationManager().stopAnimation(crate);
            plugin.getMenuManager().openCrateEditorMenu(player, crate);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cAnimation has been reset."));
            return;
        }

        if (slot == 30) {
            player.closeInventory();
            plugin.getMenuManager().removeOpenMenu(player);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPlease type input value"));

            plugin.getChatInputManager().setPendingInput(player, (input) -> {
                int tick;
                try {
                    tick = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Invalid number."));
                    plugin.getMenuManager().openPickColorMenu(player, crate);
                    return;
                }
                if (tick < 1) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Tick must be at least 1."));
                    plugin.getMenuManager().openPickColorMenu(player, crate);
                    return;
                }
                crate.getAnimationData().setTick(tick);
                plugin.getCrateManager().saveCrate(crate);
                if (crate.getAnimationData().hasAnimation()) {
                    plugin.getAnimationManager().restartAnimation(crate);
                }
                plugin.getMenuManager().openPickColorMenu(player, crate);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aTick set to " + tick + "."));
            }, () -> {
                plugin.getMenuManager().openPickColorMenu(player, crate);
            });
            return;
        }

        String colorCode = MenuManager.getColorCodeFromSlot(slot);
        if (colorCode != null) {
            AnimationData data = crate.getAnimationData();
            data.setColor(colorCode);
            plugin.getCrateManager().saveCrate(crate);
            if (data.hasAnimation()) {
                plugin.getAnimationManager().restartAnimation(crate);
            }
            plugin.getMenuManager().openCrateEditorMenu(player, crate);
            String colorName = MenuManager.getColorName(colorCode);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aAnimation color set to " + colorCode + colorName + "&a!"));
            return;
        }
    }

    private void handleHologramTextClick(InventoryClickEvent event, Player player, Crate crate, Inventory topInv) {
        ClickType clickType = event.getClick();
        List<String> lines = crate.getHologramLines();
        int selectedLine = plugin.getHologramEditManager().getSelectedLine(player, crate.getName());

        if (clickType == ClickType.SHIFT_LEFT) {
            // Add a new line via chat input
            final int insertIndex = selectedLine + 1;
            player.closeInventory();
            plugin.getMenuManager().removeOpenMenu(player);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPlease type the new line..."));

            plugin.getChatInputManager().setPendingInput(player, (input) -> {
                lines.add(insertIndex, input);
                crate.setHologramLines(lines);
                plugin.getHologramEditManager().setSelectedLine(player, crate.getName(), insertIndex);
                plugin.getCrateManager().saveCrate(crate);
                if (crate.getBlockLocation() != null) {
                    plugin.getHologramManager().updateHologram(crate);
                }
                plugin.getMenuManager().openCrateEditorMenu(player, crate);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aHologram line added!"));
            }, () -> {
                plugin.getMenuManager().openCrateEditorMenu(player, crate);
            });
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            if (lines.size() > 1) {
                lines.remove(selectedLine);
                crate.setHologramLines(lines);
                plugin.getHologramEditManager().setSelectedLine(player, crate.getName(), 0);
                plugin.getCrateManager().saveCrate(crate);
                if (crate.getBlockLocation() != null) {
                    plugin.getHologramManager().updateHologram(crate);
                }
                topInv.setItem(7, plugin.getMenuManager().createHologramTextItem(crate, player));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCannot delete the last line."));
            }
        } else if (clickType == ClickType.LEFT) {
            if (selectedLine < lines.size() - 1) {
                selectedLine++;
            } else {
                selectedLine = 0;
            }
            plugin.getHologramEditManager().setSelectedLine(player, crate.getName(), selectedLine);
            topInv.setItem(7, plugin.getMenuManager().createHologramTextItem(crate, player));
        } else if (clickType == ClickType.RIGHT) {
            if (selectedLine > 0) {
                selectedLine--;
            } else {
                selectedLine = lines.size() - 1;
            }
            plugin.getHologramEditManager().setSelectedLine(player, crate.getName(), selectedLine);
            topInv.setItem(7, plugin.getMenuManager().createHologramTextItem(crate, player));
        } else if (clickType == ClickType.MIDDLE) {
            final int lineToEdit = selectedLine;
            final String currentText = lines.get(lineToEdit);

            player.closeInventory();
            plugin.getMenuManager().removeOpenMenu(player);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&aPlease type out new text for this line or click this message to"));

            net.md_5.bungee.api.chat.TextComponent copyComponent = new net.md_5.bungee.api.chat.TextComponent(
                    ChatColor.translateAlternateColorCodes('&', "&fCopy current text"));
            net.md_5.bungee.api.chat.ClickEvent clickEvent = new net.md_5.bungee.api.chat.ClickEvent(
                    net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND, currentText);
            copyComponent.setClickEvent(clickEvent);
            player.spigot().sendMessage(copyComponent);

            plugin.getChatInputManager().setPendingInput(player, (input) -> {
                lines.set(lineToEdit, input);
                crate.setHologramLines(lines);
                plugin.getCrateManager().saveCrate(crate);
                if (crate.getBlockLocation() != null) {
                    plugin.getHologramManager().updateHologram(crate);
                }
                plugin.getMenuManager().openCrateEditorMenu(player, crate);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aHologram line updated!"));
            }, () -> {
                plugin.getMenuManager().openCrateEditorMenu(player, crate);
            });
        }
    }

    private void handleHologramHeightEdit(Player player, Crate crate) {
        player.closeInventory();
        plugin.getMenuManager().removeOpenMenu(player);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPlease type input value."));

        plugin.getChatInputManager().setPendingInput(player, (input) -> {
            double offset;
            try {
                offset = Double.parseDouble(input);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Invalid number."));
                plugin.getMenuManager().openCrateEditorMenu(player, crate);
                return;
            }

            crate.setHologramHeightOffset(offset);
            plugin.getCrateManager().saveCrate(crate);
            if (crate.getBlockLocation() != null) {
                plugin.getHologramManager().updateHologram(crate);
            }
            plugin.getMenuManager().openCrateEditorMenu(player, crate);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aHologram height offset set to &f" + offset + "&a blocks."));
        }, () -> {
            plugin.getMenuManager().openCrateEditorMenu(player, crate);
        });
    }

    private void handleRewardEditorClick(InventoryClickEvent event, Player player, MenuManager.OpenMenu openMenu, Inventory topInv) {
        String crateName = openMenu.getCrateName();
        Crate crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInv)) {
            return;
        }

        int slot = event.getRawSlot();
        ClickType clickType = event.getClick();

        // Slot 16: Back button
        if (slot == 16) {
            plugin.getRewardEditManager().stopEdit(player);
            plugin.getMenuManager().openCrateEditorMenu(player, crate);
            return;
        }

        RewardEditManager.EditingState state = plugin.getRewardEditManager().getEditState(player);
        if (state == null) return;
        int previewSlot = state.previewSlot;
        RewardData rd = crate.getRewardData(previewSlot);

        switch (slot) {
            case 10:
                if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT) {
                    handleChanceEdit(player, crate, previewSlot);
                }
                break;

            case 11:
                if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT) {
                    rd.setGiveItem(!rd.isGiveItem());
                    plugin.getCrateManager().saveCrate(crate);
                    updateGiveItemSlot(topInv, rd);
                }
                break;

            case 12:
                handleCommandsClick(event, player, crate, previewSlot, topInv, rd);
                break;
        }
    }

    private void handleCommandsClick(InventoryClickEvent event, Player player, Crate crate, int previewSlot, Inventory topInv, RewardData rd) {
        ClickType clickType = event.getClick();
        List<String> commands = rd.getCommands();
        int selectedCmd = plugin.getRewardEditManager().getEditState(player).selectedCommand;

        if (clickType == ClickType.SHIFT_LEFT) {
            player.closeInventory();
            plugin.getMenuManager().removeOpenMenu(player);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPlease input a value."));

            plugin.getChatInputManager().setPendingInput(player, (input) -> {
                commands.add(input);
                rd.setCommands(commands);
                plugin.getCrateManager().saveCrate(crate);
                plugin.getMenuManager().openRewardEditorMenu(player, crate, previewSlot);
                animateCommandItem(player, crate, previewSlot);
            }, () -> {
                plugin.getMenuManager().openRewardEditorMenu(player, crate, previewSlot);
            });
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            if (commands.isEmpty()) return;
            if (selectedCmd >= 0 && selectedCmd < commands.size()) {
                commands.remove(selectedCmd);
                rd.setCommands(commands);
                plugin.getCrateManager().saveCrate(crate);
                plugin.getRewardEditManager().setSelectedCommand(player, 0);
                topInv.setItem(12, plugin.getMenuManager().createCommandsItem(crate, previewSlot, player));
            }
        } else if (clickType == ClickType.LEFT) {
            if (selectedCmd < commands.size() - 1) {
                selectedCmd++;
            } else {
                selectedCmd = 0;
            }
            plugin.getRewardEditManager().setSelectedCommand(player, selectedCmd);
            topInv.setItem(12, plugin.getMenuManager().createCommandsItem(crate, previewSlot, player));
        } else if (clickType == ClickType.RIGHT) {
            if (selectedCmd > 0) {
                selectedCmd--;
            } else {
                selectedCmd = commands.size() - 1;
            }
            plugin.getRewardEditManager().setSelectedCommand(player, selectedCmd);
            topInv.setItem(12, plugin.getMenuManager().createCommandsItem(crate, previewSlot, player));
        }
    }

    private void animateCommandItem(Player player, Crate crate, int previewSlot) {
        Inventory topInv = player.getOpenInventory().getTopInventory();

        ItemStack bedrock = new ItemStack(Material.BEDROCK);
        ItemMeta bedrockMeta = bedrock.getItemMeta();
        bedrockMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&lLoading."));
        bedrock.setItemMeta(bedrockMeta);
        topInv.setItem(12, bedrock);

        String[] frames = {"&a&lLoading.", "&a&lLoading..", "&a&lLoading...", "&a&lLoading..", "&a&lLoading."};

        for (int i = 0; i < frames.length; i++) {
            final String frameName = frames[i];
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                Inventory currentInv = player.getOpenInventory().getTopInventory();
                ItemStack current = currentInv.getItem(12);
                if (current != null && current.getType() == Material.BEDROCK) {
                    ItemMeta meta = current.getItemMeta();
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', frameName));
                    current.setItemMeta(meta);
                    currentInv.setItem(12, current);
                }
            }, (long) (i * 14));
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            Inventory currentInv = player.getOpenInventory().getTopInventory();
            ItemStack current = currentInv.getItem(12);
            if (current != null && current.getType() == Material.BEDROCK) {
                currentInv.setItem(12, plugin.getMenuManager().createCommandsItem(crate, previewSlot, player));
            }
        }, 60L);
    }

    private void updateGiveItemSlot(Inventory inv, RewardData rd) {
        ItemStack giveItem = new ItemStack(Material.HOPPER);
        ItemMeta giveMeta = giveItem.getItemMeta();
        giveMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lGive Item"));
        List<String> giveLore = new ArrayList<>();
        giveLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        if (rd.isGiveItem()) {
            giveLore.add(ChatColor.translateAlternateColorCodes('&', "&6Current: &aYes"));
        } else {
            giveLore.add(ChatColor.translateAlternateColorCodes('&', "&6Current: &cNo"));
        }
        giveMeta.setLore(giveLore);
        giveItem.setItemMeta(giveMeta);
        inv.setItem(11, giveItem);
    }

    private void handleChanceEdit(Player player, Crate crate, int previewSlot) {
        player.closeInventory();
        plugin.getMenuManager().removeOpenMenu(player);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPlease type a chance for this reward:"));

        plugin.getChatInputManager().setPendingInput(player, (input) -> {
            double chance;
            try {
                chance = Double.parseDouble(input);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Invalid number."));
                plugin.getMenuManager().openRewardEditorMenu(player, crate, previewSlot);
                return;
            }

            if (chance < 0 || chance > 100) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Chance must be between 0 and 100."));
                plugin.getMenuManager().openRewardEditorMenu(player, crate, previewSlot);
                return;
            }

            RewardData rd = crate.getRewardData(previewSlot);
            rd.setChance(chance);
            plugin.getCrateManager().saveCrate(crate);
            plugin.getMenuManager().openRewardEditorMenu(player, crate, previewSlot);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aChance set to &f" + chance + "%&a."));
        }, () -> {
            plugin.getMenuManager().openRewardEditorMenu(player, crate, previewSlot);
        });
    }

    private void handleKeyEdit(Player player, Crate crate) {
        player.closeInventory();
        plugin.getMenuManager().removeOpenMenu(player);

        // Place the key item in hotbar slot 4 (index 3, zero-based)
        ItemStack keyItem = crate.getKeyItem().clone();
        keyItem.setAmount(1);

        player.getInventory().setItem(3, keyItem);
        player.updateInventory();

        plugin.getKeyEditManager().startKeyEdit(player, crate.getName());

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aYou have received a key in your hand to modify to your liking or"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&ayou can get a completely new key, right click the item to complete!"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aLeft Click to change the Crate item"));
    }

    private void handleRowsEdit(Player player, Crate crate) {
        player.closeInventory();
        plugin.getMenuManager().removeOpenMenu(player);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPlease type the rows 1-6"));

        plugin.getChatInputManager().setPendingInput(player, (input) -> {
            int rows;
            try {
                rows = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Invalid number of rows."));
                plugin.getMenuManager().openCrateEditorMenu(player, crate);
                return;
            }

            if (rows < 1 || rows > 6) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Invalid number of rows."));
                plugin.getMenuManager().openCrateEditorMenu(player, crate);
                return;
            }

            crate.setPreviewRows(rows);

            int maxPreviewSlots = rows * 9;
            List<ItemStack> previewItems = crate.getPreviewItems();
            while (previewItems.size() > maxPreviewSlots) {
                previewItems.remove(previewItems.size() - 1);
            }

            plugin.getCrateManager().saveCrate(crate);
            plugin.getMenuManager().openCrateEditorMenu(player, crate);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPreview rows set to &f" + rows + "&a."));
        }, () -> {
            plugin.getMenuManager().openCrateEditorMenu(player, crate);
        });
    }

    private void handleDisplayNameEdit(Player player, Crate crate) {
        player.closeInventory();
        plugin.getMenuManager().removeOpenMenu(player);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPlease type the new displayname..."));

        plugin.getChatInputManager().setPendingInput(player, (input) -> {
            crate.setDisplayName(input);
            plugin.getCrateManager().saveCrate(crate);

            if (crate.getBlockLocation() != null) {
                plugin.getHologramManager().updateHologram(crate);
            }

            plugin.getMenuManager().openCrateEditorMenu(player, crate);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aDisplay name updated!"));
        }, () -> {
            plugin.getMenuManager().openCrateEditorMenu(player, crate);
        });
    }

    private boolean isGlassPane(Material material) {
        return material == Material.STAINED_GLASS_PANE || material == Material.THIN_GLASS;
    }

    private void savePreviewItems(Inventory inv, Crate crate) {
        int size = inv.getSize();
        List<ItemStack> previewItems = new ArrayList<>();

        for (int i = 9; i < size; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null) {
                plugin.getMenuManager().stripPreviewLore(item);
                int previewSlot = i - 9;
                // FIX 2: Auto-set glass panes to 0% chance
                if (isGlassPane(item.getType())) {
                    RewardData rd = crate.getRewardData(previewSlot);
                    rd.setChance(0.0);
                }
                previewItems.add(item.clone());
            } else {
                previewItems.add(null);
            }
        }

        while (!previewItems.isEmpty() && previewItems.get(previewItems.size() - 1) == null) {
            previewItems.remove(previewItems.size() - 1);
        }

        crate.setPreviewItems(previewItems);
        plugin.getCrateManager().saveCrate(crate);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        MenuManager.OpenMenu openMenu = plugin.getMenuManager().getOpenMenu(player);
        if (openMenu == null) return;

        String menuType = openMenu.getType();

        if (menuType.equals("preview") || menuType.equals("editor") || menuType.equals("reward_editor") || menuType.equals("pick_color")) {
            event.setCancelled(true);
        } else if (menuType.equals("crate_editor")) {
            boolean editing = plugin.getMenuManager().isEditingMode(player, openMenu.getCrateName());
            if (editing) {
                event.setCancelled(true);
            } else {
                for (int dragSlot : event.getRawSlots()) {
                    if (dragSlot < 9) {
                        event.setCancelled(true);
                        return;
                    }
                }
                Crate crate = plugin.getCrateManager().getCrate(openMenu.getCrateName());
                if (crate != null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        savePreviewItems(event.getView().getTopInventory(), crate);
                    }, 1L);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        MenuManager.OpenMenu openMenu = plugin.getMenuManager().getOpenMenu(player);
        if (openMenu == null) return;

        if (openMenu.getType().equals("crate_editor")) {
            Crate crate = plugin.getCrateManager().getCrate(openMenu.getCrateName());
            if (crate != null) {
                boolean editing = plugin.getMenuManager().isEditingMode(player, crate.getName());
                if (!editing) {
                    savePreviewItems(event.getInventory(), crate);
                }
            }
        }

        if (openMenu.getType().equals("reward_editor")) {
            plugin.getRewardEditManager().stopEdit(player);
        }

        plugin.getMenuManager().removeOpenMenu(player);
    }
}
