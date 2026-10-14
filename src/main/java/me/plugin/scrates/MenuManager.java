package me.plugin.scrates;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuManager {

    private sCrates plugin;
    private Map<String, OpenMenu> openMenus;
    private Map<String, Boolean> editingMode;

    public MenuManager(sCrates plugin) {
        this.plugin = plugin;
        this.openMenus = new HashMap<>();
        this.editingMode = new HashMap<>();
    }

    public void openEditorMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, ChatColor.translateAlternateColorCodes('&', "&8Crate Editor"));

        ItemStack glassPane = createGlassPane();

        int[] glassSlots = {0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
        for (int slot : glassSlots) {
            inv.setItem(slot, glassPane);
        }

        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta starMeta = star.getItemMeta();
        starMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aCreate a new Crate"));
        star.setItemMeta(starMeta);
        inv.setItem(4, star);

        int[] crateSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        List<Crate> crates = new ArrayList<>(plugin.getCrateManager().getCrates().values());
        for (int i = 0; i < crates.size() && i < crateSlots.length; i++) {
            Crate crate = crates.get(i);
            ItemStack keyItem = crate.getKeyItem().clone();
            ItemMeta meta = keyItem.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to edit key."));
            meta.setLore(lore);
            keyItem.setItemMeta(meta);
            inv.setItem(crateSlots[i], keyItem);
        }

        player.openInventory(inv);
        setOpenMenu(player, new OpenMenu("editor", null));
    }

    public void openCrateEditorMenu(Player player, Crate crate) {
        int size = crate.getMenuSize();
        String title = ChatColor.translateAlternateColorCodes('&', "&8" + crate.getName() + " Editor");
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Slot 0: Virtual Mode Toggle
        // Bug 3 fix: Virtual ON = green tint, Virtual OFF = redstone (red indicator)
        ItemStack virtualToggle;
        if (crate.isVirtualMode()) {
            virtualToggle = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 13); // Green stained glass pane
            ItemMeta vMeta = virtualToggle.getItemMeta();
            vMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&lVirtual Mode: ON"));
            virtualToggle.setItemMeta(vMeta);
        } else {
            virtualToggle = new ItemStack(Material.REDSTONE);
            ItemMeta vMeta = virtualToggle.getItemMeta();
            vMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4&lVirtual Mode: OFF"));
            virtualToggle.setItemMeta(vMeta);
        }
        inv.setItem(0, virtualToggle);

        // Slot 1: Key Item Display
        ItemStack keyDisplay = crate.getKeyItem().clone();
        ItemMeta keyMeta = keyDisplay.getItemMeta();
        List<String> keyLore = keyMeta.hasLore() ? new ArrayList<>(keyMeta.getLore()) : new ArrayList<>();
        keyLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        keyLore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to edit key."));
        keyMeta.setLore(keyLore);
        keyDisplay.setItemMeta(keyMeta);
        inv.setItem(1, keyDisplay);

        // Slot 2: Preview Menu Rows
        ItemStack rowsItem = new ItemStack(Material.REDSTONE_COMPARATOR);
        ItemMeta rowsMeta = rowsItem.getItemMeta();
        rowsMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lPreview Menu Rows"));
        List<String> rowsLore = new ArrayList<>();
        rowsLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        rowsLore.add(ChatColor.translateAlternateColorCodes('&', "&6Rows: " + crate.getPreviewRows()));
        rowsLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        rowsLore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to edit Rows"));
        rowsMeta.setLore(rowsLore);
        rowsItem.setItemMeta(rowsMeta);
        inv.setItem(2, rowsItem);

        // Slot 4: Editing Mode Toggle
        // isEditingMode = true means currently in editing/layout mode (Lever shown to quit)
        // isEditingMode = false means currently in free-move mode (Torch shown to start editing)
        boolean editing = isEditingMode(player, crate.getName());
        ItemStack toggleItem;
        if (editing) {
            // Currently editing (Lever mode) - show Torch to go back to free-move
            toggleItem = new ItemStack(Material.REDSTONE_TORCH_ON);
            ItemMeta toggleMeta = toggleItem.getItemMeta();
            toggleMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lQuit Editing Layout"));
            toggleItem.setItemMeta(toggleMeta);
        } else {
            // Currently in free-move mode - show Lever to start editing
            toggleItem = new ItemStack(Material.LEVER);
            ItemMeta toggleMeta = toggleItem.getItemMeta();
            toggleMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&lStart Editing Layout"));
            toggleItem.setItemMeta(toggleMeta);
        }
        inv.setItem(4, toggleItem);

        // Slot 5: Animation (Nether Quartz)
        ItemStack animItem = createAnimationQuartzItem(crate);
        inv.setItem(5, animItem);

        // Slot 6: Display Name
        ItemStack nameItem = new ItemStack(Material.NAME_TAG);
        ItemMeta nameMeta = nameItem.getItemMeta();
        nameMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lDisplay name"));
        List<String> nameLore = new ArrayList<>();
        nameLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        nameLore.add(ChatColor.translateAlternateColorCodes('&', "&6Current: " + crate.getDisplayNameColored()));
        nameLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        nameLore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to edit display name."));
        nameMeta.setLore(nameLore);
        nameItem.setItemMeta(nameMeta);
        inv.setItem(6, nameItem);

        // Slot 7: Hologram Text
        ItemStack holoItem = createHologramTextItem(crate, player);
        inv.setItem(7, holoItem);

        // Slot 8: Animation Visible (Anvil)
        ItemStack animVisibleItem = createAnimationVisibleItem(crate, player);
        inv.setItem(8, animVisibleItem);

        // Fill preview slots (9 onward)
        List<ItemStack> previewItems = crate.getPreviewItems();
        boolean isEditing = isEditingMode(player, crate.getName());
        if (previewItems != null) {
            for (int i = 0; i < previewItems.size() && (9 + i) < size; i++) {
                if (previewItems.get(i) != null) {
                    ItemStack displayItem = previewItems.get(i).clone();
                    if (isEditing) {
                        addPreviewLore(displayItem, crate, i);
                    }
                    inv.setItem(9 + i, displayItem);
                }
            }
        }

        player.openInventory(inv);
        setOpenMenu(player, new OpenMenu("crate_editor", crate.getName()));
    }

    public void openPreviewMenu(Player player, Crate crate) {
        int size = crate.getPreviewMenuSize();
        if (size < 9) size = 9;
        String title = ChatColor.translateAlternateColorCodes('&', "&8" + crate.getDisplayName());
        Inventory inv = Bukkit.createInventory(null, size, title);

        List<ItemStack> previewItems = crate.getPreviewItems();
        if (previewItems != null) {
            for (int i = 0; i < previewItems.size() && i < size; i++) {
                if (previewItems.get(i) != null) {
                    inv.setItem(i, previewItems.get(i).clone());
                }
            }
        }

        player.openInventory(inv);
        setOpenMenu(player, new OpenMenu("preview", crate.getName()));
    }

    @SuppressWarnings("deprecation")
    public void openRewardEditorMenu(Player player, Crate crate, int previewSlot) {
        ItemStack rewardItem = crate.getPreviewItems().get(previewSlot);
        String title;
        if (rewardItem != null && rewardItem.hasItemMeta() && rewardItem.getItemMeta().hasDisplayName()) {
            title = rewardItem.getItemMeta().getDisplayName();
        } else {
            title = ChatColor.translateAlternateColorCodes('&', "&8Reward Editor");
        }

        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Fill all empty slots with black stained glass panes
        ItemStack blackPane = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta blackMeta = blackPane.getItemMeta();
        blackMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7 "));
        blackPane.setItemMeta(blackMeta);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, blackPane);
        }

        // Slot 10: Chance (EXP_BOTTLE)
        RewardData rd = crate.getRewardData(previewSlot);
        ItemStack chanceItem = new ItemStack(Material.EXP_BOTTLE);
        ItemMeta chanceMeta = chanceItem.getItemMeta();
        chanceMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lChance"));
        List<String> chanceLore = new ArrayList<>();
        chanceLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        chanceLore.add(ChatColor.translateAlternateColorCodes('&', "&6Current: &a" + rd.getChance() + "%"));
        chanceLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        chanceLore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to edit chance."));
        chanceMeta.setLore(chanceLore);
        chanceItem.setItemMeta(chanceMeta);
        inv.setItem(10, chanceItem);

        // Slot 11: Give Item (Hopper)
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

        // Slot 12: Commands (COMMAND_MINECART)
        ItemStack cmdItem = createCommandsItem(crate, previewSlot, player);
        inv.setItem(12, cmdItem);

        // Slot 13: Exact copy of the reward item
        if (rewardItem != null) {
            inv.setItem(13, rewardItem.clone());
        }

        // Slot 16: Back (Bed)
        ItemStack backItem = new ItemStack(Material.BED);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lBack"));
        backItem.setItemMeta(backMeta);
        inv.setItem(16, backItem);

        player.openInventory(inv);
        setOpenMenu(player, new OpenMenu("reward_editor", crate.getName()));
        plugin.getRewardEditManager().startEdit(player, crate.getName(), previewSlot);
    }

    public ItemStack createCommandsItem(Crate crate, int previewSlot, Player player) {
        ItemStack cmdItem = new ItemStack(Material.COMMAND_MINECART);
        ItemMeta cmdMeta = cmdItem.getItemMeta();
        cmdMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lCommands"));

        RewardData rd = crate.getRewardData(previewSlot);
        List<String> commands = rd.getCommands();
        int selectedCmd = plugin.getRewardEditManager() != null && plugin.getRewardEditManager().isEditingReward(player)
                ? plugin.getRewardEditManager().getEditState(player).selectedCommand : 0;
        if (selectedCmd >= commands.size()) selectedCmd = 0;

        List<String> cmdLore = new ArrayList<>();
        cmdLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        cmdLore.add(ChatColor.translateAlternateColorCodes('&', "&7&lPlaceholders"));
        cmdLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        cmdLore.add(ChatColor.translateAlternateColorCodes('&', "&6%playerId%: &f<player UUID>"));
        cmdLore.add(ChatColor.translateAlternateColorCodes('&', "&6%player%: &f<player name>"));
        cmdLore.add(ChatColor.translateAlternateColorCodes('&', "&6%amount%: &f<item stack size in preview>"));
        cmdLore.add(ChatColor.translateAlternateColorCodes('&', "&6%crate%: &f" + crate.getDisplayNameColored()));
        cmdLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));

        for (int i = 0; i < commands.size(); i++) {
            String prefix = (i == selectedCmd) ? "&a" : "&7";
            cmdLore.add(ChatColor.translateAlternateColorCodes('&', prefix + i + ". &f" + commands.get(i)));
        }

        cmdLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        cmdLore.add(ChatColor.translateAlternateColorCodes('&', "&aShift Left Click to add a new command"));
        cmdLore.add(ChatColor.translateAlternateColorCodes('&', "&cShift Right Click to delete a command"));
        cmdLore.add(ChatColor.translateAlternateColorCodes('&', "&eLeft and Right Click to Scroll"));

        cmdMeta.setLore(cmdLore);
        cmdItem.setItemMeta(cmdMeta);
        return cmdItem;
    }

    public ItemStack createHologramTextItem(Crate crate, Player player) {
        ItemStack holoItem = new ItemStack(Material.SKULL_ITEM, 1, (short) 1); // Wither Skeleton Skull
        ItemMeta holoMeta = holoItem.getItemMeta();
        holoMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lHologram Text"));

        List<String> lines = crate.getHologramLines();
        int selectedLine = plugin.getHologramEditManager().getSelectedLine(player, crate.getName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        for (int i = 0; i < lines.size(); i++) {
            String prefix = (i == selectedLine) ? "&a" : "&7";
            String lineText = lines.get(i);
            // For %displayName% line, show literal text, do NOT resolve
            lore.add(ChatColor.translateAlternateColorCodes('&', prefix + i + ". " + lineText));
        }
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&aShift Left Click to add a new line"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&cShift Right Click to delete a line"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eLeft and Right Click to Scroll"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&bMiddle Click to edit text"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&9Press Q to Up and Down Hologram"));

        holoMeta.setLore(lore);
        holoItem.setItemMeta(holoMeta);
        return holoItem;
    }

    @SuppressWarnings("deprecation")
    public void addPreviewLore(ItemStack item, Crate crate, int previewSlot) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Save original lore for restoration when editing mode is toggled back
        List<String> originalLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        // Check if we already added preview lore (has "Click to edit reward." at the end)
        if (!originalLore.isEmpty() && originalLore.get(originalLore.size() - 1).equals(ChatColor.translateAlternateColorCodes('&', "&eClick to edit reward."))) {
            return; // already has preview lore
        }

        RewardData rd = crate.getRewardData(previewSlot);
        String status = rd.isGiveItem() ? "&aYes" : "&cNo";

        List<String> lore = new ArrayList<>(originalLore);
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&6Chance: &a" + rd.getChance() + "%"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&6Give Item: &f" + ChatColor.translateAlternateColorCodes('&', status)));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&6Commands: &f" + rd.getCommands().size()));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to edit reward."));

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public void stripPreviewLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return;

        List<String> lore = new ArrayList<>(meta.getLore());
        String marker = ChatColor.translateAlternateColorCodes('&', "&eClick to edit reward.");
        for (int i = lore.size() - 1; i >= 0; i--) {
            if (lore.get(i).equals(marker)) {
                // Remove 6 lines: &7, chance, giveItem, commands, &7, marker (indices i-5 through i)
                int start = Math.max(0, i - 5);
                for (int j = i; j >= start; j--) {
                    lore.remove(j);
                }
                break;
            }
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private ItemStack createGlassPane() {
        ItemStack pane = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7 "));
        pane.setItemMeta(meta);
        return pane;
    }

    public void setOpenMenu(Player player, OpenMenu menu) {
        openMenus.put(player.getUniqueId().toString(), menu);
    }

    public OpenMenu getOpenMenu(Player player) {
        return openMenus.get(player.getUniqueId().toString());
    }

    public void removeOpenMenu(Player player) {
        openMenus.remove(player.getUniqueId().toString());
    }

    public boolean hasOpenMenu(Player player) {
        return openMenus.containsKey(player.getUniqueId().toString());
    }

    public boolean isEditingMode(Player player, String crateName) {
        String key = player.getUniqueId().toString() + ":" + crateName.toLowerCase();
        return editingMode.getOrDefault(key, false);
    }

    public void setEditingMode(Player player, String crateName, boolean editing) {
        String key = player.getUniqueId().toString() + ":" + crateName.toLowerCase();
        editingMode.put(key, editing);
    }

    public void toggleEditingMode(Player player, String crateName) {
        setEditingMode(player, crateName, !isEditingMode(player, crateName));
    }

    @SuppressWarnings("deprecation")
    public ItemStack createAnimationQuartzItem(Crate crate) {
        ItemStack item = new ItemStack(Material.QUARTZ);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lAnimation"));
        AnimationData data = crate.getAnimationData();
        String animDisplay;
        if (data.hasColor()) {
            animDisplay = data.getColor() + getColorName(data.getColor());
        } else {
            animDisplay = "&cNone";
        }
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&6Tick: &c" + data.getTick()));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&6Animation: " + animDisplay));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to edit animation"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    public ItemStack createAnimationVisibleItem(Crate crate, Player player) {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lAnimation Visible"));
        List<String> lines = crate.getHologramLines();
        int selectedLine = plugin.getAnimationEditManager().getSelectedLine(player, crate.getName());
        AnimationData data = crate.getAnimationData();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7This crate hologram have " + lines.size() + " lines"));
        for (int i = 0; i < lines.size(); i++) {
            String prefix = (i == selectedLine) ? "&a" : "&7";
            String lineText = lines.get(i);
            boolean animated = data.isLineAnimated(i);
            if (animated) {
                lore.add(ChatColor.translateAlternateColorCodes('&', prefix + "&m" + i + ". " + lineText));
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', prefix + i + ". " + lineText));
            }
        }
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&aShift + Left Click to setup animation in the line"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&cShift + Right Click to delete animation in the line"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eLeft and Right to Scroll"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&bMiddle Click to setup animation only works on DisplayName"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    public void openPickColorMenu(Player player, Crate crate) {
        Inventory inv = Bukkit.createInventory(null, 36, ChatColor.translateAlternateColorCodes('&', "&8Pick a color"));
        ItemStack blackPane = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta blackMeta = blackPane.getItemMeta();
        blackMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7 "));
        blackPane.setItemMeta(blackMeta);
        for (int i = 0; i < 36; i++) { inv.setItem(i, blackPane); }

        int[][] row2 = {{10,11},{11,13},{12,9},{13,14},{14,10},{15,1},{16,7}};
        String[] row2Names = {"&1Dark Blue","&2Dark Green","&3Dark Aqua","&4Dark Red","&5Dark Purple","&6Gold","&7Gray"};
        for (int i = 0; i < row2.length; i++) {
            ItemStack wool = new ItemStack(Material.WOOL, 1, (short) row2[i][1]);
            ItemMeta m = wool.getItemMeta();
            m.setDisplayName(ChatColor.translateAlternateColorCodes('&', row2Names[i]));
            wool.setItemMeta(m);
            inv.setItem(row2[i][0], wool);
        }

        int[][] row3 = {{19,15},{20,11},{21,5},{22,3},{23,14},{24,2},{25,4}};
        String[] row3Names = {"&8Dark Gray","&9Blue","&aGreen","&bAqua","&cRed","&dLight Purple","&eYellow"};
        for (int i = 0; i < row3.length; i++) {
            ItemStack wool = new ItemStack(Material.WOOL, 1, (short) row3[i][1]);
            ItemMeta m = wool.getItemMeta();
            m.setDisplayName(ChatColor.translateAlternateColorCodes('&', row3Names[i]));
            wool.setItemMeta(m);
            inv.setItem(row3[i][0], wool);
        }

        ItemStack whiteWool = new ItemStack(Material.WOOL, 1, (short) 0);
        ItemMeta wm = whiteWool.getItemMeta();
        wm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fWhite"));
        whiteWool.setItemMeta(wm);
        inv.setItem(31, whiteWool);

        ItemStack tnt = new ItemStack(Material.TNT);
        ItemMeta tntMeta = tnt.getItemMeta();
        tntMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lReset Animation"));
        tnt.setItemMeta(tntMeta);
        inv.setItem(29, tnt);

        ItemStack watch = new ItemStack(Material.WATCH);
        ItemMeta watchMeta = watch.getItemMeta();
        watchMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lTicks"));
        AnimationData data = crate.getAnimationData();
        List<String> watchLore = new ArrayList<>();
        watchLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        watchLore.add(ChatColor.translateAlternateColorCodes('&', "&6Current Ticks: " + data.getTick()));
        watchLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        watchLore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to edit."));
        watchMeta.setLore(watchLore);
        watch.setItemMeta(watchMeta);
        inv.setItem(30, watch);

        ItemStack backItem = new ItemStack(Material.BED);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lBack"));
        backItem.setItemMeta(backMeta);
        inv.setItem(3, backItem);

        player.openInventory(inv);
        setOpenMenu(player, new OpenMenu("pick_color", crate.getName()));
    }

    public static String getColorName(String code) {
        switch (code) {
            case "&0": return "Black";
            case "&1": return "Dark Blue";
            case "&2": return "Dark Green";
            case "&3": return "Dark Aqua";
            case "&4": return "Dark Red";
            case "&5": return "Dark Purple";
            case "&6": return "Gold";
            case "&7": return "Gray";
            case "&8": return "Dark Gray";
            case "&9": return "Blue";
            case "&a": return "Green";
            case "&b": return "Aqua";
            case "&c": return "Red";
            case "&d": return "Light Purple";
            case "&e": return "Yellow";
            case "&f": return "White";
            default: return "None";
        }
    }

    public static String getColorCodeFromSlot(int slot) {
        switch (slot) {
            case 10: return "&1";
            case 11: return "&2";
            case 12: return "&3";
            case 13: return "&4";
            case 14: return "&5";
            case 15: return "&6";
            case 16: return "&7";
            case 19: return "&8";
            case 20: return "&9";
            case 21: return "&a";
            case 22: return "&b";
            case 23: return "&c";
            case 24: return "&d";
            case 25: return "&e";
            case 31: return "&f";
            default: return null;
        }
    }

    public static class OpenMenu {
        private String type;
        private String crateName;

        public OpenMenu(String type, String crateName) {
            this.type = type;
            this.crateName = crateName;
        }

        public String getType() {
            return type;
        }

        public String getCrateName() {
            return crateName;
        }
    }
}
