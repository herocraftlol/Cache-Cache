package com.hideseek.cachecache.stats;

import com.hideseek.cachecache.CacheCachePlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Suit le nombre de victoires de chaque joueur en tant que Seeker et en tant que caché
 * (hider), persisté dans stats.yml pour survivre aux redémarrages du serveur.
 */
public class StatsManager {

    public static class PlayerStats {
        public String name;
        public int seekerWins;
        public int hiderWins;

        public PlayerStats(String name, int seekerWins, int hiderWins) {
            this.name = name;
            this.seekerWins = seekerWins;
            this.hiderWins = hiderWins;
        }
    }

    private final CacheCachePlugin plugin;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();
    private final File file;

    public StatsManager(CacheCachePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (!cfg.isConfigurationSection("players")) return;
        for (String uuidStr : cfg.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String name = cfg.getString("players." + uuidStr + ".name", uuidStr);
                int seekerWins = cfg.getInt("players." + uuidStr + ".seekerWins", 0);
                int hiderWins = cfg.getInt("players." + uuidStr + ".hiderWins", 0);
                stats.put(uuid, new PlayerStats(name, seekerWins, hiderWins));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> e : stats.entrySet()) {
            String path = "players." + e.getKey();
            cfg.set(path + ".name", e.getValue().name);
            cfg.set(path + ".seekerWins", e.getValue().seekerWins);
            cfg.set(path + ".hiderWins", e.getValue().hiderWins);
        }
        try {
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Impossible de sauvegarder stats.yml : " + ex.getMessage());
        }
    }

    private PlayerStats getOrCreate(Player p) {
        return stats.computeIfAbsent(p.getUniqueId(), id -> new PlayerStats(p.getName(), 0, 0));
    }

    public void recordSeekerWin(Player p) {
        PlayerStats s = getOrCreate(p);
        s.name = p.getName();
        s.seekerWins++;
        save();
    }

    public void recordHiderWin(Player p) {
        PlayerStats s = getOrCreate(p);
        s.name = p.getName();
        s.hiderWins++;
        save();
    }

    public List<Map.Entry<UUID, PlayerStats>> getTopSeekers(int limit) {
        return stats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().seekerWins, a.getValue().seekerWins))
                .filter(e -> e.getValue().seekerWins > 0)
                .limit(limit)
                .toList();
    }

    public List<Map.Entry<UUID, PlayerStats>> getTopHiders(int limit) {
        return stats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().hiderWins, a.getValue().hiderWins))
                .filter(e -> e.getValue().hiderWins > 0)
                .limit(limit)
                .toList();
    }
}
