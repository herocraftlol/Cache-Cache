package com.hideseek.cachecache.gui;

import com.hideseek.cachecache.map.GameMap;
import com.hideseek.cachecache.map.Scenario;
import com.hideseek.cachecache.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ScenarioGui {

    public static final String TITLE_PREFIX = "§8✦ Scénarios: ";
    public static final NamespacedKey KEY_SCENARIO = new NamespacedKey("cachecache", "gui_scenario");

    public static Inventory build(GameMap map) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 18, Msg.of(TITLE_PREFIX + map.getName()));

        Scenario[] all = Scenario.values();
        for (int i = 0; i < all.length; i++) {
            Scenario s = all[i];
            boolean active = map.getScenarios().contains(s);

            ItemStack item = new ItemStack(s.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Msg.of(s.getDisplayName()));

            List<Component> lore = new ArrayList<>();
            lore.add(Msg.of(s.getFlavor()));
            lore.add(Msg.of(" "));
            for (String line : wrap(s.getDescription(), 38)) {
                lore.add(Msg.of("§7" + line));
            }
            lore.add(Msg.of(" "));
            lore.add(Msg.of(active ? "§a§l✔ ACTIVÉ" : "§c§l✘ DÉSACTIVÉ"));
            lore.add(Msg.of("§8▸ Clique pour " + (active ? "désactiver" : "activer")));
            meta.lore(lore);

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

    /**
     * Découpe une description trop longue en plusieurs lignes de lore (Minecraft ne
     * retourne pas la lore automatiquement).
     */
    private static List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() + word.length() + 1 > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) current.append(" ");
            current.append(word);
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }
}
