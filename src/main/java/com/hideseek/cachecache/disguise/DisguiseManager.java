package com.hideseek.cachecache.disguise;

import com.hideseek.cachecache.CacheCachePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Déguisement 100% natif, sans aucune dépendance externe.
 *
 * Le principe : le VRAI joueur reçoit un effet d'Invisibilité permanent (invisible pour
 * TOUT LE MONDE, y compris lui-même à la 3e personne — c'est le comportement standard de
 * l'invisibilité sur un joueur), et un vrai mob "fantôme" (sans IA, increvable, sans
 * collision, sans gravité) apparaît à sa place et le suit à chaque tick. Résultat : tout
 * le monde, LE JOUEUR DÉGUISÉ Y COMPRIS, ne voit que le mob — plus personne ne voit son
 * skin.
 *
 * Le combat se fait en frappant le mob fantôme : c'est au code appelant (PlayerListener)
 * de vérifier via {@link #getPlayerBehindShadow(UUID)} si le mob frappé est le fantôme
 * d'un joueur caché, pour appliquer l'élimination sur le bon joueur.
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
     * vrai joueur invisible pour tout le monde, lui compris.
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

        // Invisibilité permanente et silencieuse : cache le vrai joueur pour tout le monde,
        // lui compris (il ne voit donc plus son skin, seulement le mob fantôme).
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
    }

    /** Retire le déguisement : supprime le mob fantôme et rend le joueur visible à nouveau. */
    public void undisguise(Player player) {
        UUID shadowId = playerToShadow.remove(player.getUniqueId());
        if (shadowId != null) {
            shadowToPlayer.remove(shadowId);
            Entity shadow = Bukkit.getEntity(shadowId);
            if (shadow != null) shadow.remove();
        }
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
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
