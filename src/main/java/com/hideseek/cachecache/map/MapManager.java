package com.hideseek.cachecache.map;

import com.hideseek.cachecache.CacheCachePlugin;
import com.hideseek.cachecache.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MapManager {

    private final CacheCachePlugin plugin;
    private final Map<String, GameMap> maps = new LinkedHashMap<>();
    private final File mapsFolder;
    private Location hub;

    public MapManager(CacheCachePlugin plugin) {
        this.plugin = plugin;
        this.mapsFolder = new File(plugin.getDataFolder(), "maps");
        if (!mapsFolder.exists()) mapsFolder.mkdirs();
        loadAll();
        loadHub();
    }

    public Collection<GameMap> getMaps() { return maps.values(); }

    public GameMap getMap(String name) {
        return maps.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return maps.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public GameMap createMap(String name) {
        GameMap map = new GameMap(name);
        maps.put(name.toLowerCase(Locale.ROOT), map);
        save(map);
        return map;
    }

    public void deleteMap(String name) {
        GameMap map = maps.remove(name.toLowerCase(Locale.ROOT));
        if (map != null) {
            File f = new File(mapsFolder, name.toLowerCase(Locale.ROOT) + ".yml");
            if (f.exists()) f.delete();
        }
    }

    /**
     * Renomme une map existante : met à jour son nom interne, déplace son fichier de
     * sauvegarde, et met à jour la clé utilisée dans le cache en mémoire.
     */
    public boolean renameMap(String oldName, String newName) {
        GameMap map = getMap(oldName);
        if (map == null) return false;
        if (exists(newName)) return false;

        maps.remove(oldName.toLowerCase(Locale.ROOT));
        File oldFile = new File(mapsFolder, oldName.toLowerCase(Locale.ROOT) + ".yml");
        if (oldFile.exists()) oldFile.delete();

        map.setName(newName);
        maps.put(newName.toLowerCase(Locale.ROOT), map);
        save(map);
        return true;
    }

    public Location getHub() { return hub; }

    public void setHub(Location hub) {
        this.hub = hub;
        File f = new File(plugin.getDataFolder(), "hub.yml");
        YamlConfiguration cfg = new YamlConfiguration();
        LocationUtil.save(cfg, "hub", hub);
        try {
            cfg.save(f);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder le hub: " + e.getMessage());
        }
    }

    private void loadHub() {
        File f = new File(plugin.getDataFolder(), "hub.yml");
        if (!f.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        hub = LocationUtil.load(cfg, "hub");
    }

    private void loadAll() {
        File[] files = mapsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                GameMap map = load(f);
                if (map != null) maps.put(map.getName().toLowerCase(Locale.ROOT), map);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur en chargeant la map " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    private GameMap load(File f) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        String name = cfg.getString("name");
        if (name == null) return null;
        GameMap map = new GameMap(name);

        map.setPos1(LocationUtil.load(cfg, "pos1"));
        map.setPos2(LocationUtil.load(cfg, "pos2"));
        if (cfg.getBoolean("posConfirmed", false)) map.confirmPos();

        map.setSpawnSeeker(LocationUtil.load(cfg, "spawnSeeker"));
        map.setLobby(LocationUtil.load(cfg, "lobby"));

        map.setTimeTicks(cfg.getInt("timeTicks", -1));
        map.setKillMaxBase(cfg.getInt("killMaxBase", 10));
        map.setKillMaxPerHider(cfg.getInt("killMaxPerHider", 5));
        map.setMaxPlayers(cfg.getInt("maxPlayers", -1));
        map.setSeekerCount(cfg.getInt("seekerCount", 1));
        map.setDecoyBase(cfg.getInt("decoyBase", 12));
        map.setDecoyPerHider(cfg.getInt("decoyPerHider", 4));
        map.setVirusMode(cfg.getBoolean("virusMode", false));
        map.setSaved(cfg.getBoolean("saved", false));

        if (cfg.isConfigurationSection("mobs")) {
            for (String mobName : cfg.getConfigurationSection("mobs").getKeys(false)) {
                map.getMobPercentages().put(mobName, cfg.getInt("mobs." + mobName + ".percent"));
                map.getMobExplicit().put(mobName, cfg.getBoolean("mobs." + mobName + ".explicit"));
            }
        }

        if (cfg.isList("hunts")) {
            for (Map<?, ?> h : (List<Map<?, ?>>) (List<?>) cfg.getMapList("hunts")) {
                int tick = ((Number) h.get("tick")).intValue();
                int count = ((Number) h.get("count")).intValue();
                map.getHunts().add(new GameMap.HuntEntry(tick, count));
            }
        }

        if (cfg.isList("scenarios")) {
            for (String s : cfg.getStringList("scenarios")) {
                try { map.getScenarios().add(Scenario.valueOf(s)); } catch (IllegalArgumentException ignored) {}
            }
        }

        return map;
    }

    public void save(GameMap map) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("name", map.getName());

        LocationUtil.save(cfg, "pos1", map.getPos1());
        LocationUtil.save(cfg, "pos2", map.getPos2());
        cfg.set("posConfirmed", map.isPosConfirmed());

        LocationUtil.save(cfg, "spawnSeeker", map.getSpawnSeeker());
        LocationUtil.save(cfg, "lobby", map.getLobby());

        cfg.set("timeTicks", map.getTimeTicks());
        cfg.set("killMaxBase", map.getKillMaxBase());
        cfg.set("killMaxPerHider", map.getKillMaxPerHider());
        cfg.set("maxPlayers", map.getMaxPlayers());
        cfg.set("seekerCount", map.getSeekerCount());
        cfg.set("decoyBase", map.getDecoyBase());
        cfg.set("decoyPerHider", map.getDecoyPerHider());
        cfg.set("virusMode", map.isVirusMode());
        cfg.set("saved", map.isSaved());

        for (Map.Entry<String, Integer> e : map.getMobPercentages().entrySet()) {
            cfg.set("mobs." + e.getKey() + ".percent", e.getValue());
            cfg.set("mobs." + e.getKey() + ".explicit", map.getMobExplicit().getOrDefault(e.getKey(), false));
        }

        List<Map<String, Object>> huntsList = new ArrayList<>();
        for (GameMap.HuntEntry h : map.getHunts()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tick", h.triggerTick);
            m.put("count", h.fireworkCount);
            huntsList.add(m);
        }
        cfg.set("hunts", huntsList);

        List<String> scenarioNames = new ArrayList<>();
        for (Scenario s : map.getScenarios()) scenarioNames.add(s.name());
        cfg.set("scenarios", scenarioNames);

        try {
            cfg.save(new File(mapsFolder, map.getName().toLowerCase(Locale.ROOT) + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder la map " + map.getName() + ": " + e.getMessage());
        }
    }
}
