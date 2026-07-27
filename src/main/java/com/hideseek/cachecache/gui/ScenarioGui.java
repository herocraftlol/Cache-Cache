package com.hideseek.cachecache.gui;

import com.hideseek.cachecache.util.Msg;

import com.hideseek.cachecache.map.GameMap;
import com.hideseek.cachecache.map.Scenario;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ScenarioGui {

    public static final String TITLE_PREFIX = "§8Scénarios: ";
    public static final NamespacedKey KEY_SCENARIO = new NamespacedKey("cachecache", "gui_scenario");

    public static Inventory build(GameMap map) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 18, Msg.of(TITLE_PREFIX + map.getName()));

        Scenario[] all = Scenario.values();
        for (int i = 0; i < all.length; i++) {
            Scenario s = all[i];
            boolean active = map.getScenarios().contains(s);

            ItemStack item = new ItemStack(s.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Msg.of((active ? "§a" : "§7") + s.getDisplayName()));
            meta.lore(java.util.List.of(
                    Msg.of("§7" + s.getDescription()),
                    Msg.of(active ? "§a✔ Activé" : "§c✘ Désactivé")
            ));
            if (active) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.getPersistentDataContainer().set(KEY_SCENARIO, PersistentDataType.STRING, s.name());
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
        return inv;
    }
}
