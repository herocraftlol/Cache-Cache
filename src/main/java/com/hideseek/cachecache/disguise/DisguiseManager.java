package com.hideseek.cachecache.disguise;

import com.hideseek.cachecache.CacheCachePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Déguisement 100% natif, sans aucune dépendance externe (plus besoin de LibsDisguises).
 *
 * Le principe : on cache le VRAI joueur (invisible pour tout le monde via
 * {@link Player#hidePlayer}), et on fait apparaître à sa place un vrai mob "fantôme"
 * (sans IA, increvable, sans collision) qu'on téléporte sur la position du joueur à
 * chaque tick. Pour tout le monde (Seeker compris), seul ce mob est visible et
 * cliquable — le vrai joueur, lui, n'existe plus sur les clients des autres joueurs, donc
 * personne ne peut accidentellement cliquer sur "la vraie personne" au lieu du mob.
 *
 * Le combat se fait donc en frappant le mob fantôme : c'est au code appelant
 * (PlayerListener) de vérifier via {@link #getPlayerBehindShadow(UUID)} si le mob frappé
 * est le fantôme d'un joueur caché, pour appliquer l'élimination sur le bon joueur.
 */
public class DisguiseManager {

    private final CacheCachePlugin plugin;

    private final Map<UUID, UUID> playerToShadow = new HashMap<>();
    private final Map<UUID, UUID> shadowToPlayer = new HashMap<>();

    private BukkitTask syncTask;

    public DisguiseManager(CacheCachePlugin plugin) {
        this.plugin = plugin;
    }

    /** Toujours vrai désormais : le camouflage ne dépend plus d'aucun plugin tiers. */
    public boolean isAvailable() { return true; }

    public void start() {
        syncTask = Bukkit.getScheduler().runTaskTimer(plugin, this::syncAll, 1L, 1L);
    }

    public void stop() {
        if (syncTask != null) syncTask.cancel();
        for (UUID playerId : new java.util.ArrayList<>(playerToShadow.keySet())) {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null) {
                undisguise(p);
            } else {
                UUID shadowId = playerToShadow.get(playerId);
                Entity shadow = shadowId != null ? Bukkit.getEntity(shadowId) : null;
                if (shadow != null) shadow.remove();
            }
        }
        playerToShadow.clear();
        shadowToPlayer.clear();
    }

    /**
     * Déguise le joueur en mob : fait apparaître un vrai mob fantôme à sa place et rend le
     * vrai joueur invisible pour tout le monde.
     */
    public void disguiseAsMob(Player player, EntityType type) {
        undisguise(player); // enlève un éventuel déguisement précédent (ex: mob swap)

        Location loc = player.getLocation();
        Entity shadow;
        try {
            shadow = loc.getWorld().spawnEntity(loc, type);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Impossible de faire apparaître un mob de type " + type + " (type invalide/non invocable).");
            return;
        }

        if (shadow instanceof LivingEntity living) {
            living.setAI(false);
            living.setCollidable(false);
            living.setSilent(true);
            living.setRemoveWhenFarAway(false);
            living.setPersistent(true);
            living.setGravity(false);
            try { living.setCanPickupItems(false); } catch (Throwable ignored) {}
        }
        shadow.setCustomNameVisible(false);

        playerToShadow.put(player.getUniqueId(), shadow.getUniqueId());
        shadowToPlayer.put(shadow.getUniqueId(), player.getUniqueId());

        // Le joueur déguisé ne doit JAMAIS voir son propre mob fantôme (sinon il voit son
        // propre corps ET le mob superposés/légèrement désynchronisés = impression de
        // "bouger tout seul"). Il se voit juste normalement (à la 3e personne).
        player.hideEntity(plugin, shadow);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                viewer.hidePlayer(plugin, player);
            }
        }
    }

    /** Retire le déguisement : supprime le mob fantôme et rend le joueur visible à nouveau. */
    public void undisguise(Player player) {
        UUID shadowId = playerToShadow.remove(player.getUniqueId());
        if (shadowId != null) {
            shadowToPlayer.remove(shadowId);
            Entity shadow = Bukkit.getEntity(shadowId);
            if (shadow != null) shadow.remove();
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer(plugin, player);
        }
    }

    /**
     * Si l'entité donnée est le mob fantôme d'un joueur caché, renvoie l'UUID de ce
     * joueur. Sinon renvoie null (c'est alors un simple mob de décor).
     */
    public UUID getPlayerBehindShadow(UUID entityId) {
        return shadowToPlayer.get(entityId);
    }

    public boolean isDisguised(Player player) {
        return playerToShadow.containsKey(player.getUniqueId());
    }

    /** À appeler quand un joueur se connecte : il faut lui cacher les joueurs déjà déguisés. */
    public void applyHiddenStateFor(Player newViewer) {
        for (UUID playerId : playerToShadow.keySet()) {
            Player hiddenPlayer = Bukkit.getPlayer(playerId);
            if (hiddenPlayer != null && !hiddenPlayer.equals(newViewer)) {
                newViewer.hidePlayer(plugin, hiddenPlayer);
            }
        }
    }

    private void syncAll() {
        if (playerToShadow.isEmpty()) return;
        for (Map.Entry<UUID, UUID> entry : new HashMap<>(playerToShadow).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            Entity shadow = Bukkit.getEntity(entry.getValue());
            if (player == null || shadow == null) continue;
            Location loc = player.getLocation();
            shadow.teleport(loc);
            // Neutralise toute vélocité/chute résiduelle que le moteur physique aurait pu
            // appliquer au fantôme entre deux synchronisations (collision avec un bloc,
            // hitbox différente de celle du joueur dans un passage étroit, etc.).
            shadow.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            shadow.setFallDistance(0f);
        }
    }
}
