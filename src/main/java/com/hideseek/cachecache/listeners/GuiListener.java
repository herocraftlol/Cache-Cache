package com.hideseek.cachecache.listeners;

import com.hideseek.cachecache.util.Msg;

import com.hideseek.cachecache.CacheCachePlugin;
import com.hideseek.cachecache.gui.MapListGui;
import com.hideseek.cachecache.gui.ScenarioGui;
import com.hideseek.cachecache.map.GameMap;
import com.hideseek.cachecache.map.Scenario;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class GuiListener implements Listener {

    private final CacheCachePlugin plugin;

    public GuiListener(CacheCachePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().title() != null ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.getView().title()) : "";

        if (title.equals(stripSection(MapListGui.TITLE))) {
            e.setCancelled(true);
            handleMapListClick(e);
        } else if (title.startsWith(stripSection(ScenarioGui.TITLE_PREFIX))) {
            e.setCancelled(true);
            handleScenarioClick(e, title.substring(stripSection(ScenarioGui.TITLE_PREFIX).length()));
        }
    }

    private String stripSection(String s) {
        return s.replace("§8", "").replace("§7", "").replace("§6", "").replace("§c", "").replace("§a", "");
    }

    private void handleMapListClick(InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        Player p = (Player) e.getWhoClicked();

        if (item.getItemMeta().getPersistentDataContainer().has(MapListGui.KEY_RANDOM_JOIN, PersistentDataType.STRING)) {
            p.closeInventory();
            boolean joined = plugin.getGameManager().quickJoinBest(p, null);
            if (!joined) p.sendMessage(Msg.of("§cAucune arène disponible pour le moment."));
            return;
        }

        String mapName = item.getItemMeta().getPersistentDataContainer().get(MapListGui.KEY_MAP, PersistentDataType.STRING);
        if (mapName == null) return;
        GameMap map = plugin.getMapManager().getMap(mapName);
        if (map == null) return;
        p.closeInventory();
        plugin.getGameManager().joinGame(p, map);
    }

    private void handleScenarioClick(InventoryClickEvent e, String mapName) {
        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String scenarioName = item.getItemMeta().getPersistentDataContainer().get(ScenarioGui.KEY_SCENARIO, PersistentDataType.STRING);
        if (scenarioName == null) return;

        GameMap map = plugin.getMapManager().getMap(mapName);
        if (map == null) return;

        Scenario scenario = Scenario.valueOf(scenarioName);
        if (map.getScenarios().contains(scenario)) {
            map.getScenarios().remove(scenario);
        } else {
            map.getScenarios().add(scenario);
        }
        plugin.getMapManager().save(map);

        Player p = (Player) e.getWhoClicked();
        e.getInventory().setContents(ScenarioGui.build(map).getContents());
        p.sendMessage(Msg.of("§7Scénario §f" + scenario.getDisplayName() + " §7" +
                (map.getScenarios().contains(scenario) ? "§aactivé" : "§cdésactivé")));
    }
}
