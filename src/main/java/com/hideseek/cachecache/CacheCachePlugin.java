package com.hideseek.cachecache;

import com.hideseek.cachecache.commands.CCCommand;
import com.hideseek.cachecache.disguise.DisguiseManager;
import com.hideseek.cachecache.game.GameManager;
import com.hideseek.cachecache.hologram.HologramManager;
import com.hideseek.cachecache.listeners.GuiListener;
import com.hideseek.cachecache.listeners.MapProtectionListener;
import com.hideseek.cachecache.listeners.PlayerListener;
import com.hideseek.cachecache.map.MapManager;
import com.hideseek.cachecache.stats.StatsManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CacheCachePlugin extends JavaPlugin {

    private MapManager mapManager;
    private GameManager gameManager;
    private DisguiseManager disguiseManager;
    private StatsManager statsManager;
    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        this.disguiseManager = new DisguiseManager(this);
        this.mapManager = new MapManager(this);
        this.gameManager = new GameManager(this);
        this.statsManager = new StatsManager(this);
        this.hologramManager = new HologramManager(this);
        this.disguiseManager.start();

        CCCommand ccCommand = new CCCommand(this);
        getCommand("cc").setExecutor(ccCommand);
        getCommand("cc").setTabCompleter(ccCommand);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new MapProtectionListener(this), this);

        // Recrée les hologrammes de classement sauvegardés après un redémarrage.
        getServer().getScheduler().runTaskLater(this, this.hologramManager::restoreAll, 20L);

        getLogger().info("CacheCache activé !");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            for (var session : gameManager.getAllSessions()) {
                session.stopTicking();
            }
        }
        if (disguiseManager != null) disguiseManager.stop();
        getLogger().info("CacheCache désactivé.");
    }

    public MapManager getMapManager() { return mapManager; }
    public GameManager getGameManager() { return gameManager; }
    public DisguiseManager getDisguiseManager() { return disguiseManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public HologramManager getHologramManager() { return hologramManager; }
}
