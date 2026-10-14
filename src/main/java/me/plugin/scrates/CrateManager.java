package me.plugin.scrates;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CrateManager {

    private sCrates plugin;
    private File cratesFolder;
    private Map<String, Crate> crates;

    public CrateManager(sCrates plugin) {
        this.plugin = plugin;
        this.crates = new LinkedHashMap<>();
        this.cratesFolder = new File(plugin.getDataFolder(), "Crates");
        if (!cratesFolder.exists()) {
            cratesFolder.mkdirs();
        }
    }

    public File getCratesFolder() {
        return cratesFolder;
    }

    public Crate getCrate(String name) {
        return crates.get(name.toLowerCase());
    }

    public boolean crateExists(String name) {
        return crates.containsKey(name.toLowerCase());
    }

    public Map<String, Crate> getCrates() {
        return crates;
    }

    public Crate createCrate(String name) {
        Crate crate = new Crate(name);
        crates.put(name.toLowerCase(), crate);
        saveCrate(crate);
        return crate;
    }

    public void deleteCrate(String name) {
        Crate crate = crates.remove(name.toLowerCase());
        if (crate != null) {
            File file = new File(cratesFolder, name + ".yml");
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public List<String> listCrateNames() {
        List<String> names = new ArrayList<>();
        for (Crate crate : crates.values()) {
            names.add(crate.getName());
        }
        return names;
    }

    public void loadAllCrates() {
        crates.clear();
        File[] files = cratesFolder.listFiles((dir, filename) -> filename.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String crateName = file.getName().replace(".yml", "");
            try {
                Crate crate = loadCrate(crateName, file);
                if (crate != null) {
                    crates.put(crateName.toLowerCase(), crate);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load crate: " + crateName + " - " + e.getMessage());
            }
        }
    }

    private Crate loadCrate(String name, File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Crate crate = new Crate(name);

        if (config.contains("displayName")) {
            crate.setDisplayName(config.getString("displayName"));
        }

        if (config.contains("previewRows")) {
            crate.setPreviewRows(config.getInt("previewRows"));
        }

        if (config.contains("blockLocation")) {
            ConfigurationSection locSec = config.getConfigurationSection("blockLocation");
            String worldName = locSec.getString("world");
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                double x = locSec.getDouble("x");
                double y = locSec.getDouble("y");
                double z = locSec.getDouble("z");
                crate.setBlockLocation(new Location(world, x, y, z));
            }
        }

        if (config.contains("keyItem")) {
            ItemStack keyItem = deserializeItemStack(config.getConfigurationSection("keyItem"));
            if (keyItem != null) {
                crate.setKeyItem(keyItem);
            }
        }

        if (config.contains("previewItems")) {
            ConfigurationSection previewSec = config.getConfigurationSection("previewItems");
            List<ItemStack> items = new ArrayList<>();
            Set<String> keys = previewSec.getKeys(false);
            List<Integer> sortedKeys = new ArrayList<>();
            for (String key : keys) {
                try {
                    sortedKeys.add(Integer.parseInt(key));
                } catch (NumberFormatException ignored) {}
            }
            java.util.Collections.sort(sortedKeys);

            for (int slot : sortedKeys) {
                ConfigurationSection itemSec = previewSec.getConfigurationSection(String.valueOf(slot));
                if (itemSec != null) {
                    ItemStack item = deserializeItemStack(itemSec);
                    if (item != null) {
                        while (items.size() <= slot) {
                            items.add(null);
                        }
                        items.set(slot, item);
                    }
                }
            }
            crate.setPreviewItems(items);
        }

        if (config.contains("hologramLines")) {
            crate.setHologramLines(config.getStringList("hologramLines"));
        }

        if (config.contains("virtualMode")) {
            crate.setVirtualMode(config.getBoolean("virtualMode"));
        }

        if (config.contains("hologramHeightOffset")) {
            crate.setHologramHeightOffset(config.getDouble("hologramHeightOffset", 0.0));
        }

        if (config.contains("rewardData")) {
            ConfigurationSection rdSec = config.getConfigurationSection("rewardData");
            Map<Integer, RewardData> rewardDataMap = new HashMap<>();
            for (String slotKey : rdSec.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotKey);
                    ConfigurationSection slotSec = rdSec.getConfigurationSection(slotKey);
                    if (slotSec != null) {
                        double chance = slotSec.getDouble("chance", 90.0);
                        boolean giveItem = slotSec.getBoolean("giveItem", true);
                        List<String> commands = slotSec.getStringList("commands");
                        if (commands.isEmpty() && slotSec.contains("commands")) {
                            commands = slotSec.getStringList("commands");
                        }
                        RewardData rd = new RewardData(chance, giveItem, commands);
                        rewardDataMap.put(slot, rd);
                    }
                } catch (NumberFormatException ignored) {}
            }
            crate.setRewardData(rewardDataMap);
        }

        if (config.contains("animation")) {
            ConfigurationSection animSec = config.getConfigurationSection("animation");
            AnimationData animData = new AnimationData();
            animData.setColor(animSec.getString("color", null));
            animData.setTick(animSec.getInt("tick", 10));
            List<Integer> animLines = animSec.getIntegerList("lines");
            for (int line : animLines) {
                animData.addLine(line);
            }
            animData.setDisplayNameOnly(animSec.getBoolean("displayNameOnly", false));
            animData.setDisplayNameAnimated(animSec.getBoolean("displayNameAnimated", false));
            crate.setAnimationData(animData);
        }

        return crate;
    }

    public void saveCrate(Crate crate) {
        File file = new File(cratesFolder, crate.getName() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("name", crate.getName());
        config.set("displayName", crate.getDisplayName());
        config.set("previewRows", crate.getPreviewRows());

        if (crate.getBlockLocation() != null) {
            Location loc = crate.getBlockLocation();
            config.set("blockLocation.world", loc.getWorld().getName());
            config.set("blockLocation.x", loc.getX());
            config.set("blockLocation.y", loc.getY());
            config.set("blockLocation.z", loc.getZ());
        }

        if (crate.getKeyItem() != null) {
            ConfigurationSection keySec = config.createSection("keyItem");
            serializeItemStack(crate.getKeyItem(), keySec);
        }

        List<ItemStack> previewItems = crate.getPreviewItems();
        if (previewItems != null && !previewItems.isEmpty()) {
            ConfigurationSection previewSec = config.createSection("previewItems");
            for (int i = 0; i < previewItems.size(); i++) {
                ItemStack item = previewItems.get(i);
                if (item != null) {
                    ConfigurationSection itemSec = previewSec.createSection(String.valueOf(i));
                    serializeItemStack(item, itemSec);
                }
            }
        }

        config.set("hologramLines", crate.getHologramLines());
        config.set("virtualMode", crate.isVirtualMode());
        config.set("hologramHeightOffset", crate.getHologramHeightOffset());

        Map<Integer, RewardData> rewardDataMap = crate.getRewardData();
        if (rewardDataMap != null && !rewardDataMap.isEmpty()) {
            ConfigurationSection rdSec = config.createSection("rewardData");
            for (Map.Entry<Integer, RewardData> entry : rewardDataMap.entrySet()) {
                ConfigurationSection slotSec = rdSec.createSection(String.valueOf(entry.getKey()));
                RewardData rd = entry.getValue();
                slotSec.set("chance", rd.getChance());
                slotSec.set("giveItem", rd.isGiveItem());
                slotSec.set("commands", rd.getCommands());
            }
        }

        AnimationData animData = crate.getAnimationData();
        if (animData != null) {
            ConfigurationSection animSec = config.createSection("animation");
            animSec.set("color", animData.getColor() != null ? animData.getColor() : "");
            animSec.set("tick", animData.getTick());
            animSec.set("lines", new ArrayList<>(animData.getSortedLines()));
            animSec.set("displayNameOnly", animData.isDisplayNameOnly());
            animSec.set("displayNameAnimated", animData.isDisplayNameAnimated());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crate: " + crate.getName() + " - " + e.getMessage());
        }
    }

    public void saveAllCrates() {
        for (Crate crate : crates.values()) {
            saveCrate(crate);
        }
    }

    public void serializeItemStack(ItemStack item, ConfigurationSection section) {
        section.set("material", item.getType().name());
        section.set("amount", item.getAmount());
        section.set("durability", (int) item.getDurability());

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                section.set("displayName", meta.getDisplayName());
            }
            if (meta.hasLore()) {
                section.set("lore", meta.getLore());
            }
            if (meta.hasEnchants()) {
                ConfigurationSection enchSec = section.createSection("enchantments");
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    enchSec.set(entry.getKey().getName(), entry.getValue());
                }
            }
        }
    }

    public ItemStack deserializeItemStack(ConfigurationSection section) {
        if (section == null) return null;

        String materialName = section.getString("material");
        if (materialName == null) return null;

        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return null;
        }

        int amount = section.getInt("amount", 1);
        short durability = (short) section.getInt("durability", 0);

        ItemStack item = new ItemStack(material, amount, durability);

        if (section.contains("displayName") || section.contains("lore") || section.contains("enchantments")) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (section.contains("displayName")) {
                    meta.setDisplayName(section.getString("displayName"));
                }
                if (section.contains("lore")) {
                    meta.setLore(section.getStringList("lore"));
                }
                item.setItemMeta(meta);

                if (section.contains("enchantments")) {
                    ConfigurationSection enchSec = section.getConfigurationSection("enchantments");
                    if (enchSec != null) {
                        for (String enchName : enchSec.getKeys(false)) {
                            Enchantment ench = Enchantment.getByName(enchName);
                            if (ench != null) {
                                int level = enchSec.getInt(enchName);
                                item.addUnsafeEnchantment(ench, level);
                            }
                        }
                    }
                }
            }
        }

        return item;
    }

    public Crate getCrateByLocation(Location loc) {
        for (Crate crate : crates.values()) {
            if (crate.getBlockLocation() != null) {
                Location crateLoc = crate.getBlockLocation();
                if (crateLoc.getWorld().equals(loc.getWorld())
                        && crateLoc.getBlockX() == loc.getBlockX()
                        && crateLoc.getBlockY() == loc.getBlockY()
                        && crateLoc.getBlockZ() == loc.getBlockZ()) {
                    return crate;
                }
            }
        }
        return null;
    }
}
