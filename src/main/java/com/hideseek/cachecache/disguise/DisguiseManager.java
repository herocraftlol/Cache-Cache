package com.hideseek.cachecache.disguise;

import com.hideseek.cachecache.CacheCachePlugin;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Gestionnaire de camouflage : utilise le backend natif (vrai mob "fantôme") qui est
 * compatible avec tous les serveurs sans dépendance externe.
 *
 * Le vrai joueur reçoit un effet d'Invisibilité permanent et silencieux
 * (géré ici) : il est donc invisible pour tout le monde, lui
 * compris à la 3e personne — il ne voit plus que son mob, jamais son skin.
 */
public class DisguiseManager {

    private final CacheCachePlugin plugin;
    private NativeDisguiseBackend nativeBackend;

    public DisguiseManager(CacheCachePlugin plugin) {
        this.plugin = plugin;
    }

    /** Toujours vrai : le camouflage fonctionne sans ProtocolLib. */
    public boolean isAvailable() { return true; }

    /** Retourne false (backend ProtocolLib retiré car incompatible). */
    public boolean isUsingPacketBackend() { return false; }

    public void start() {
        nativeBackend = new NativeDisguiseBackend(plugin);
        nativeBackend.start();
        plugin.getLogger().info("CacheCache camouflage natif activé.");
    }

    public void stop() {
        if (nativeBackend != null) nativeBackend.stop();
    }

    public void disguiseAsMob(Player player, EntityType type) {
        if (nativeBackend != null) {
            nativeBackend.disguise(player, type);
        }
        // Invisibilité permanente et silencieuse : cache le vrai joueur pour tout le
        // monde, lui compris (il ne voit donc plus son skin, seulement son mob).
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
    }

    public void undisguise(Player player) {
        if (nativeBackend != null) nativeBackend.undisguise(player);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    /**
     * Si l'entité donnée (identifiée par son UUID Bukkit réel) est le mob fantôme d'un
     * joueur caché, renvoie l'UUID de ce joueur.
     */
    public UUID getPlayerBehindShadow(UUID entityId) {
        return nativeBackend != null ? nativeBackend.getPlayerBehindShadow(entityId) : null;
    }

    public boolean isDisguised(Player player) {
        return nativeBackend != null && nativeBackend.isDisguised(player);
    }
}
