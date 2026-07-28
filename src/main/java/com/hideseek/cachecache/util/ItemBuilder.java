package com.hideseek.cachecache.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Petits items "tagués" via PersistentDataContainer : le tag est relu par PlayerListener
 * au clic (PlayerInteractEvent) pour déclencher l'action correspondante.
 */
public final class ItemBuilder {

    public static final NamespacedKey KEY = com.hideseek.cachecache.game.GameSession.KEY_ITEM;

    private ItemBuilder() {}

    public static ItemStack tagged(Material material, String displayName, String tag) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Msg.of(displayName));
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, tag);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack barrier(String displayName, String tag) {
        return tagged(Material.BARRIER, displayName, tag);
    }
}
