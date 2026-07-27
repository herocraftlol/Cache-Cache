package com.hideseek.cachecache.listeners;

import com.hideseek.cachecache.CacheCachePlugin;
import com.hideseek.cachecache.game.GameSession;
import com.hideseek.cachecache.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * La map ne doit jamais être dégradée PENDANT une partie en cours. En dehors d'une
 * partie (construction/édition), ou pour un joueur admin (op / permission
 * "cachecache.admin"), la map reste modifiable normalement.
 */
public class MapProtectionListener implements Listener {

    private final CacheCachePlugin plugin;

    public MapProtectionListener(CacheCachePlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isBypassing(Player p) {
        return p.isOp() || p.hasPermission("cachecache.admin");
    }

    /**
     * Vrai si le joueur participe actuellement à une partie en cours (Seeker, caché,
     * ou spectateur d'une partie RUNNING/STARTING).
     */
    private boolean isInActiveGame(Player p) {
        for (GameSession session : plugin.getGameManager().getAllSessions()) {
            if (session.getState() != GameState.RUNNING && session.getState() != GameState.STARTING) continue;
            if (session.getSeekers().contains(p.getUniqueId())
                    || session.getAliveHidden().contains(p.getUniqueId())
                    || session.getSpectators().contains(p.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (isBypassing(p)) return;
        if (isInActiveGame(p)) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (isBypassing(p)) return;
        if (isInActiveGame(p)) e.setCancelled(true);
    }

    // Les explosions (scénario TNT) ne doivent jamais abîmer la map, admin ou non :
    // le scénario est conçu pour ne jamais faire de dégâts de bloc.
    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().clear();
        e.setCancelled(true);
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent e) {
        if (e.getPlayer() != null && isBypassing(e.getPlayer())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBurn(BlockBurnEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        e.setCancelled(true);
    }
}
