package com.hideseek.cachecache.hologram;

import com.hideseek.cachecache.CacheCachePlugin;
import com.hideseek.cachecache.stats.StatsManager;
import com.hideseek.cachecache.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Hologrammes de classement (Seeker / Hider), sans aucune dépendance externe : de simples
 * ArmorStand invisibles empilés verticalement, un par ligne de texte. Leur emplacement est
 * sauvegardé dans holograms.yml pour être recréés automatiquement après un redémarrage.
 */
public class HologramManager {

    public enum BoardType { SEEKER, HIDER }

    private final CacheCachePlugin plugin;
    private final File file;

    private final Map<BoardType, Location> locations = new EnumMap<>(BoardType.class);
    private final Map<BoardType, List<UUID>> armorStands = new EnumMap<>(BoardType.class);

    private static final double LINE_SPACING = 0.28;

    public HologramManager(CacheCachePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "holograms.yml");
        armorStands.put(BoardType.SEEKER, new ArrayList<>());
        armorStands.put(BoardType.HIDER, new ArrayList<>());
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (BoardType type : BoardType.values()) {
            String key = type.name().toLowerCase(Locale.ROOT);
            if (!cfg.isSet(key + ".world")) continue;
            World world = Bukkit.getWorld(cfg.getString(key + ".world"));
            if (world == null) continue;
            Location loc = new Location(world, cfg.getDouble(key + ".x"), cfg.getDouble(key + ".y"), cfg.getDouble(key + ".z"));
            locations.put(type, loc);
        }
    }

    private void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<BoardType, Location> e : locations.entrySet()) {
            String key = e.getKey().name().toLowerCase(Locale.ROOT);
            Location loc = e.getValue();
            cfg.set(key + ".world", loc.getWorld().getName());
            cfg.set(key + ".x", loc.getX());
            cfg.set(key + ".y", loc.getY());
            cfg.set(key + ".z", loc.getZ());
        }
        try {
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Impossible de sauvegarder holograms.yml : " + ex.getMessage());
        }
    }

    /** Fait apparaître (ou déplace) le classement à l'endroit donné, et sauvegarde sa position. */
    public void summon(BoardType type, Location location) {
        remove(type);
        locations.put(type, location.clone());
        save();
        render(type);
    }

    /** Retire le classement (et oublie son emplacement sauvegardé). */
    public void remove(BoardType type) {
        for (UUID id : armorStands.get(type)) {
            var entity = Bukkit.getEntity(id);
            if (entity != null) entity.remove();
        }
        armorStands.get(type).clear();
        locations.remove(type);
        save();
    }

    public boolean isSummoned(BoardType type) {
        return locations.containsKey(type);
    }

    /** À appeler après chaque partie (ou au démarrage) pour rafraîchir le contenu affiché. */
    public void refreshAll() {
        for (BoardType type : BoardType.values()) {
            if (locations.containsKey(type)) render(type);
        }
    }

    /** À appeler au démarrage du plugin pour recréer les hologrammes sauvegardés. */
    public void restoreAll() {
        for (BoardType type : BoardType.values()) {
            if (locations.containsKey(type)) render(type);
        }
    }

    private void render(BoardType type) {
        Location base = locations.get(type);
        if (base == null || base.getWorld() == null) return;

        for (UUID id : armorStands.get(type)) {
            var entity = Bukkit.getEntity(id);
            if (entity != null) entity.remove();
        }
        armorStands.get(type).clear();

        StatsManager stats = plugin.getStatsManager();
        List<String> lines = new ArrayList<>();
        lines.add(type == BoardType.SEEKER ? "§c§l⚔ TOP SEEKERS ⚔" : "§a§l👤 TOP HIDERS 👤");
        lines.add("§8—————————————");

        var top = type == BoardType.SEEKER ? stats.getTopSeekers(10) : stats.getTopHiders(10);
        if (top.isEmpty()) {
            lines.add("§7Aucune victoire enregistrée");
        } else {
            int rank = 1;
            for (var entry : top) {
                int wins = type == BoardType.SEEKER ? entry.getValue().seekerWins : entry.getValue().hiderWins;
                String medal = switch (rank) {
                    case 1 -> "§6#1";
                    case 2 -> "§7#2";
                    case 3 -> "§c#3";
                    default -> "§f#" + rank;
                };
                lines.add(medal + " §f" + entry.getValue().name + " §8- §e" + wins + " victoire(s)");
                rank++;
            }
        }

        double y = base.getY() + (lines.size() - 1) * LINE_SPACING;
        for (String line : lines) {
            Location loc = new Location(base.getWorld(), base.getX(), y, base.getZ());
            ArmorStand stand = base.getWorld().spawn(loc, ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setMarker(true);
                as.setGravity(false);
                as.setCustomNameVisible(true);
                as.customName(Msg.of(line));
                as.setSmall(true);
                as.setBasePlate(false);
                as.setArms(false);
                as.setPersistent(true);
                as.setCollidable(false);
                as.setInvulnerable(true);
            });
            armorStands.get(type).add(stand.getUniqueId());
            y -= LINE_SPACING;
        }
    }
}
