package com.hideseek.cachecache;

import com.hideseek.cachecache.commands.CCCommand;
import com.hideseek.cachecache.disguise.DisguiseManager;
import com.hideseek.cachecache.game.GameManager;
import com.hideseek.cachecache.listeners.GuiListener;
import com.hideseek.cachecache.listeners.MapProtectionListener;
import com.hideseek.cachecache.listeners.PlayerListener;
import com.hideseek.cachecache.map.MapManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CacheCachePlugin extends JavaPlugin {

    private MapManager mapManager;
    private GameManager gameManager;
    private DisguiseManager disguiseManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        this.disguiseManager = new DisguiseManager(this);
        this.mapManager = new MapManager(this);
        this.gameManager = new GameManager(this);

        CCCommand ccCommand = new CCCommand(this);
        getCommand("cc").setExecutor(ccCommand);
        getCommand("cc").setTabCompleter(ccCommand);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new MapProtectionListener(), this);

        getLogger().info("CacheCache activé !");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            for (var session : gameManager.getAllSessions()) {
                session.stopTicking();
            }
        }
        getLogger().info("CacheCache désactivé.");
    }

    public MapManager getMapManager() { return mapManager; }
    public GameManager getGameManager() { return gameManager; }
    public DisguiseManager getDisguiseManager() { return disguiseManager; }
}
