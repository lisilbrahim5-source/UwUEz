package me.plugin.scrates;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CrateCommand implements CommandExecutor {

    private sCrates plugin;

    public CrateCommand(sCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Only players can use this command."));
            return true;
        }

        Player player = (Player) sender;

        // Check permission (OP or scrates.admin)
        if (!player.isOp() && !player.hasPermission("scrates.admin")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7You do not have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "editor":
                handleEditor(player);
                break;
            case "set":
                handleSet(player, args);
                break;
            case "remove":
                handleRemove(player);
                break;
            case "givekey":
                handleGiveKey(player, args);
                break;
            case "list":
                handleList(player);
                break;
            default:
                sendUsage(player);
                break;
        }

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6sCrates Commands:"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/cr editor &7- Open the crate editor menu"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/cr set <crate_name> &7- Set a crate on the targeted block"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/cr remove &7- Remove a crate from the targeted block"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/cr givekey <crate_name> <player> <quantity> &7- Give crate keys"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/cr list &7- List all crates"));
    }

    private void handleEditor(Player player) {
        plugin.getMenuManager().openEditorMenu(player);
    }

    private void handleSet(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Usage: /cr set <crate_name>"));
            return;
        }

        String crateName = args[1];
        Crate crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Crate '&f" + crateName + "&7' does not exist."));
            return;
        }

        java.util.Set<Material> transparent = null;
        Block targetBlock = player.getTargetBlock(transparent, 10);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7You must be looking at a block."));
            return;
        }

        Location blockLoc = targetBlock.getLocation();

        // Check if block already has a crate
        Crate existing = plugin.getCrateManager().getCrateByLocation(blockLoc);
        if (existing != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7That block already has a crate set on it."));
            return;
        }

        // If crate already has a location, remove old hologram
        if (crate.getBlockLocation() != null) {
            plugin.getHologramManager().removeHologram(crate.getBlockLocation());
        }

        crate.setBlockLocation(blockLoc);
        plugin.getHologramManager().createHologram(crate);
        plugin.getCrateManager().saveCrate(crate);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aCrate '&f" + crate.getName() + "&a' has been set on the targeted block."));
    }

    private void handleRemove(Player player) {
        java.util.Set<Material> transparent = null;
        Block targetBlock = player.getTargetBlock(transparent, 10);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7You must be looking at a block."));
            return;
        }

        Location blockLoc = targetBlock.getLocation();
        Crate crate = plugin.getCrateManager().getCrateByLocation(blockLoc);
        if (crate == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7That block does not have a crate set on it."));
            return;
        }

        plugin.getHologramManager().removeHologram(blockLoc);
        crate.setBlockLocation(null);
        plugin.getCrateManager().saveCrate(crate);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aCrate '&f" + crate.getName() + "&a' has been removed from the block."));
    }

    private void handleGiveKey(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Usage: /cr givekey <crate_name> <player_name> <quantity>"));
            return;
        }

        String crateName = args[1];
        String playerName = args[2];
        String quantityStr = args[3];

        Crate crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Crate '&f" + crateName + "&7' does not exist."));
            return;
        }

        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Player '&f" + playerName + "&7' is not online."));
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Invalid quantity. Please enter a valid positive integer."));
            return;
        }

        if (quantity <= 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &7Invalid quantity. Please enter a valid positive integer."));
            return;
        }

        // Create the key item without extra lore lines
        ItemStack keyItem = crate.getKeyItem().clone();
        keyItem.setAmount(quantity);

        target.getInventory().addItem(keyItem);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aGiven &f" + quantity + " &aof &f" + crate.getName() + "&a keys to &f" + target.getName() + "&a."));
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aYou have received &f" + quantity + " &aof &f" + crate.getName() + "&a keys."));
    }

    private void handleList(Player player) {
        java.util.List<String> names = plugin.getCrateManager().listCrateNames();
        if (names.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7No crates exist yet."));
            return;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Crates:"));
        for (String name : names) {
            Crate crate = plugin.getCrateManager().getCrate(name);
            if (crate != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e- &f" + crate.getName() + " &7(Display: " + crate.getDisplayNameColored() + "&7)"));
            }
        }
    }
}
