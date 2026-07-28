package com.hideseek.cachecache.gui;

import com.hideseek.cachecache.util.Msg;

import com.hideseek.cachecache.CacheCachePlugin;
import com.hideseek.cachecache.game.GameSession;
import com.hideseek.cachecache.game.GameState;
import com.hideseek.cachecache.map.GameMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class MapListGui {

    public static final String TITLE = "§8Parties Cache-Cache";
    public static final NamespacedKey KEY_MAP = new NamespacedKey("cachecache", "gui_map_name");
    public static final NamespacedKey KEY_RANDOM_JOIN = new NamespacedKey("cachecache", "gui_random_join");

    private static final int MAP_SLOTS = 45; // les 5 premières lignes ; la dernière ligne est réservée au quick-join

    public static Inventory build(CacheCachePlugin plugin) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, Msg.of(TITLE));

        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, "§7", List.of());
        for (int i = 0; i < MAP_SLOTS; i++) inv.setItem(i, filler);

        int slot = 0;
        for (GameMap map : plugin.getMapManager().getMaps()) {
            if (!map.isSaved()) continue;
            if (slot >= MAP_SLOTS) break;

            GameSession session = plugin.getGameManager().getSession(map.getName());
            GameState state = session != null ? session.getState() : GameState.LOBBY;

            ItemStack item;
            if (map.isInMaintenance()) {
                item = named(Material.ORANGE_STAINED_GLASS_PANE, "§6" + map.getName(), List.of("§7En maintenance"));
            } else if (state == GameState.RUNNING || state == GameState.STARTING) {
                item = named(Material.RED_STAINED_GLASS_PANE, "§c" + map.getName(),
                        List.of("§7En cours", "§7Cliquez pour observer"));
            } else {
                int current = session != null ? session.getLobbyPlayers().size() : 0;
                item = named(Material.LIME_STAINED_GLASS_PANE, "§a" + map.getName(),
                        List.of("§7Joueurs: §f" + current + "/" + map.getMaxPlayers(), "§7Cliquez pour rejoindre"));
            }
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(KEY_MAP, PersistentDataType.STRING, map.getName());
            item.setItemMeta(meta);

            inv.setItem(slot, item);
            slot++;
        }

        // Dernière ligne : étoiles du Nether pour rejoindre au hasard l'arène disponible
        // avec le plus de joueurs en attente.
        ItemStack randomJoin = named(Material.NETHER_STAR, "§d§lPartie rapide",
                List.of("§7Rejoint l'arène disponible", "§7avec le plus de joueurs en attente"));
        ItemMeta rMeta = randomJoin.getItemMeta();
        rMeta.getPersistentDataContainer().set(KEY_RANDOM_JOIN, PersistentDataType.STRING, "1");
        randomJoin.setItemMeta(rMeta);
        for (int i = 45; i < 54; i++) inv.setItem(i, randomJoin);

        return inv;
    }

    private static ItemStack named(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Msg.of(name));
        meta.lore(lore.stream().map(Msg::of).toList());
        item.setItemMeta(meta);
        return item;
    }
}
