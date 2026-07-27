package com.hideseek.cachecache.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/**
 * La map ne doit jamais être dégradée : on bloque toute modification de bloc
 * causée par une explosion (TNT du scénario), le feu, ou les joueurs pendant une partie.
 */
public class MapProtectionListener implements Listener {

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().clear();
        e.setCancelled(true);
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent e) {
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

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        e.setCancelled(true);
    }
}
