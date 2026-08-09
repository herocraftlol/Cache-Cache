package com.hideseek.cachecache.disguise;

import com.hideseek.cachecache.CacheCachePlugin;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Façade de camouflage : utilise le backend par paquets (ProtocolLib, aucune physique
 * possible) s'il est détecté sur le serveur, sinon retombe automatiquement sur le backend
 * natif (un vrai mob "fantôme", sans dépendance mais soumis à la physique des blocs).
 *
 * Dans les deux cas, le vrai joueur reçoit un effet d'Invisibilité permanent et silencieux
 * (géré ici, commun aux deux backends) : il est donc invisible pour tout le monde, lui
 * compris à la 3e personne — il ne voit plus que son mob, jamais son skin.
 */
public class DisguiseManager {

    private final CacheCachePlugin plugin;
    private final boolean usingPacketBackend;

    private NativeDisguiseBackend nativeBackend;
    private PacketDisguiseBackend packetBackend;

    public DisguiseManager(CacheCachePlugin plugin) {
        this.plugin = plugin;
        this.usingPacketBackend = plugin.getServer().getPluginManager().getPlugin("ProtocolLib") != null;
    }

    /** Toujours vrai : le camouflage fonctionne avec ou sans ProtocolLib. */
    public boolean isAvailable() { return true; }

    /** Vrai si le backend par paquets (ProtocolLib, zéro physique) est actif. */
    public boolean isUsingPacketBackend() { return usingPacketBackend; }

    public void start() {
        if (usingPacketBackend) {
            try {
                packetBackend = new PacketDisguiseBackend(plugin);
                packetBackend.start();
                plugin.getLogger().info("ProtocolLib détecté : camouflage par paquets activé (zéro collision possible).");
                return;
            } catch (Throwable t) {
                plugin.getLogger().warning("ProtocolLib détecté mais l'initialisation du camouflage par paquets a échoué (" +
                        t.getMessage() + "). Retour au système natif.");
            }
        }
        nativeBackend = new NativeDisguiseBackend(plugin);
        nativeBackend.start();
    }

    public void stop() {
        if (packetBackend != null) packetBackend.stop();
        if (nativeBackend != null) nativeBackend.stop();
    }

    public void disguiseAsMob(Player player, EntityType type) {
        if (packetBackend != null) {
            packetBackend.disguise(player, type);
        } else if (nativeBackend != null) {
            nativeBackend.disguise(player, type);
        }
        // Invisibilité permanente et silencieuse : cache le vrai joueur pour tout le
        // monde, lui compris (il ne voit donc plus son skin, seulement son mob).
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
    }

    public void undisguise(Player player) {
        if (packetBackend != null) packetBackend.undisguise(player);
        if (nativeBackend != null) nativeBackend.undisguise(player);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    /**
     * Si l'entité donnée (identifiée par son UUID Bukkit réel) est le mob fantôme d'un
     * joueur caché, renvoie l'UUID de ce joueur. Ne s'applique qu'au backend natif — avec
     * le backend par paquets, il n'y a pas de vraie entité, donc pas d'UUID réel : le combat
     * y est géré directement par {@link PacketDisguiseBackend} via interception de paquet.
     */
    public UUID getPlayerBehindShadow(UUID entityId) {
        return nativeBackend != null ? nativeBackend.getPlayerBehindShadow(entityId) : null;
    }

    public boolean isDisguised(Player player) {
        if (packetBackend != null) return packetBackend.isDisguised(player);
        if (nativeBackend != null) return nativeBackend.isDisguised(player);
        return false;
    }
}
