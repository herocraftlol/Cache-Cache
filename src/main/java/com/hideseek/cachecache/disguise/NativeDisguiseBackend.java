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
 * Déguisement 100% natif, sans aucune dépendance externe (utilisé si ProtocolLib n'est
 * pas installé sur le serveur).
 *
 * Le principe : un vrai mob "fantôme" (sans IA, increvable, sans collision, sans gravité)
 * apparaît à la place du joueur et le suit à chaque tick. L'invisibilité du vrai joueur
 * est gérée par {@link DisguiseManager} (commune aux deux backends).
 *
 * Limite connue : comme c'est une VRAIE entité, elle reste soumise à la physique des
 * blocs — un mob avec une hitbox plus large qu'un joueur (Vache, Cheval...) peut légèrement
 * "trembler" dans les passages étroits. Aucune collision avec les joueurs n'est possible
 * (ça, c'est bien désactivé à 100%), mais la collision avec le décor (les blocs) ne peut
 * pas être supprimée sans passer par un système de paquets (voir {@link PacketDisguiseBackend}).
 */
public class NativeDisguiseBackend {

    private final CacheCachePlugin plugin;

    private final Map<UUID, UUID> playerToShadow = new HashMap<>();
    private final Map<UUID, UUID> shadowToPlayer = new HashMap<>();

    private BukkitTask syncTask;

    public NativeDisguiseBackend(CacheCachePlugin plugin) {
        this.plugin = plugin;
    }

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

    public void disguise(Player player, EntityType type) {
        undisguise(player);

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
        // Le correctif décisif : setNoPhysics désactive TOUTE la physique de l'entité
        // (blocs ET autres entités), contrairement à setCollidable qui ne couvre que les
        // collisions entre entités. C'est ce qui causait le "bouge tout seul" persistant
        // en 1.21 : une collision joueur/fantôme que setCollidable seul ne suffisait plus
        // à bloquer. Avec setNoPhysics, le fantôme devient un pur pantin visuel, uniquement
        // déplacé par nos téléportations — plus aucune interaction physique possible.
        try { shadow.setNoPhysics(true); } catch (Throwable ignored) {}

        playerToShadow.put(player.getUniqueId(), shadow.getUniqueId());
        shadowToPlayer.put(shadow.getUniqueId(), player.getUniqueId());
    }

    public void undisguise(Player player) {
        UUID shadowId = playerToShadow.remove(player.getUniqueId());
        if (shadowId != null) {
            shadowToPlayer.remove(shadowId);
            Entity shadow = Bukkit.getEntity(shadowId);
            if (shadow != null) shadow.remove();
        }
    }

    public UUID getPlayerBehindShadow(UUID entityId) {
        return shadowToPlayer.get(entityId);
    }

    public boolean isDisguised(Player player) {
        return playerToShadow.containsKey(player.getUniqueId());
    }

    private void syncAll() {
        if (playerToShadow.isEmpty()) return;
        for (Map.Entry<UUID, UUID> entry : new HashMap<>(playerToShadow).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            Entity shadow = Bukkit.getEntity(entry.getValue());
            if (player == null || shadow == null) continue;
            Location loc = player.getLocation();
            shadow.teleport(loc);
            shadow.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            shadow.setFallDistance(0f);
            try { if (!shadow.hasNoPhysics()) shadow.setNoPhysics(true); } catch (Throwable ignored) {}
            if (shadow instanceof LivingEntity living) {
                if (living.hasAI()) living.setAI(false);
                if (living.isCollidable()) living.setCollidable(false);
                if (living.hasGravity()) living.setGravity(false);
            }
        }
    }
}
