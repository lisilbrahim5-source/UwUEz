package me.plugin.scrates;

import me.plugin.scrates.listeners.ChatListener;
import me.plugin.scrates.listeners.InventoryClickListener;
import me.plugin.scrates.listeners.PlayerInteractListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class sCrates extends JavaPlugin {

    private static sCrates instance;

    private CrateManager crateManager;
    private HologramManager hologramManager;
    private ChatInputManager chatInputManager;
    private MenuManager menuManager;
    private KeyEditManager keyEditManager;
    private HologramEditManager hologramEditManager;
    private RewardEditManager rewardEditManager;
    private AnimationManager animationManager;
    private AnimationEditManager animationEditManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        java.io.File cratesFolder = new java.io.File(getDataFolder(), "Crates");
        if (!cratesFolder.exists()) {
            cratesFolder.mkdirs();
        }

        crateManager = new CrateManager(this);
        chatInputManager = new ChatInputManager();
        hologramManager = new HologramManager(this);
        menuManager = new MenuManager(this);
        keyEditManager = new KeyEditManager(this);
        hologramEditManager = new HologramEditManager(this);
        rewardEditManager = new RewardEditManager(this);
        animationManager = new AnimationManager(this);
        animationEditManager = new AnimationEditManager(this);

        crateManager.loadAllCrates();
        hologramManager.restoreAllHolograms();
        animationManager.restoreAllAnimations();

        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

        getCommand("cr").setExecutor(new CrateCommand(this));

        getLogger().info("sCrates has been enabled!");
    }

    @Override
    public void onDisable() {
        if (crateManager != null) {
            crateManager.saveAllCrates();
        }
        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }
        if (animationManager != null) {
            animationManager.stopAllAnimations();
        }
        getLogger().info("sCrates has been disabled!");
    }

    public static sCrates getInstance() {
        return instance;
    }

    public CrateManager getCrateManager() {
        return crateManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public KeyEditManager getKeyEditManager() {
        return keyEditManager;
    }

    public HologramEditManager getHologramEditManager() {
        return hologramEditManager;
    }

    public RewardEditManager getRewardEditManager() {
        return rewardEditManager;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public AnimationEditManager getAnimationEditManager() {
        return animationEditManager;
    }
}
