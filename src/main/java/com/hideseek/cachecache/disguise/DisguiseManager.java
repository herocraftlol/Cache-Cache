package com.hideseek.cachecache.disguise;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import com.hideseek.cachecache.CacheCachePlugin;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Encapsule les appels à LibsDisguises. Le plugin doit être présent sur le serveur
 * (softdepend) pour que le déguisement fonctionne ; sinon les joueurs restent visibles
 * en tant que joueurs (le jeu reste jouable mais sans le camouflage visuel).
 */
public class DisguiseManager {

    private final CacheCachePlugin plugin;
    private final boolean available;

    public DisguiseManager(CacheCachePlugin plugin) {
        this.plugin = plugin;
        this.available = plugin.getServer().getPluginManager().getPlugin("LibsDisguises") != null;
        if (!available) {
            plugin.getLogger().warning("LibsDisguises introuvable : le camouflage visuel des joueurs en mob sera désactivé. " +
                    "Installez LibsDisguises pour la fonctionnalité complète.");
        }
    }

    public boolean isAvailable() { return available; }

    public void disguiseAsMob(Player player, EntityType type) {
        if (!available) return;
        try {
            DisguiseType dtype = DisguiseType.valueOf(type.name());
            Disguise disguise = new MobDisguise(dtype);
            disguise.setEntity(player);
            disguise.startDisguise();
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Le mob " + type + " n'a pas de déguisement correspondant dans LibsDisguises.");
        }
    }

    public void undisguise(Player player) {
        if (!available) return;
        if (DisguiseAPI.isDisguised(player)) {
            DisguiseAPI.undisguiseToAll(player);
        }
    }
}
