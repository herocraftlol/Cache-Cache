package com.hideseek.cachecache.commands;

import com.hideseek.cachecache.util.Msg;

import com.hideseek.cachecache.CacheCachePlugin;
import com.hideseek.cachecache.gui.MapListGui;
import com.hideseek.cachecache.gui.ScenarioGui;
import com.hideseek.cachecache.map.GameMap;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.stream.Collectors;

public class CCCommand implements CommandExecutor, TabCompleter {

    private final CacheCachePlugin plugin;
    private final Set<String> reserved = Set.of("create", "delete", "list", "help", "hub", "gui");

    public CCCommand(CacheCachePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String first = args[0].toLowerCase(Locale.ROOT);

        switch (first) {
            case "help" -> { sendHelp(sender); return true; }
            case "list" -> { return handleList(sender); }
            case "create" -> { return handleCreate(sender, args); }
            case "delete" -> { return handleDelete(sender, args); }
            case "hub" -> { return handleHub(sender); }
            case "gui" -> { return handleGui(sender); }
            default -> { return handleMapSubcommand(sender, args); }
        }
    }

    // ------------------------------------------------------------- HELP

    private void sendHelp(CommandSender s) {
        s.sendMessage(Msg.of("§6§l=== Aide Cache-Cache ==="));
        s.sendMessage(Msg.of("§e/cc create <map> §7- Créer une nouvelle map"));
        s.sendMessage(Msg.of("§e/cc delete <map> §7- Supprimer une map (confirmation requise)"));
        s.sendMessage(Msg.of("§e/cc list §7- Lister les maps existantes"));
        s.sendMessage(Msg.of("§e/cc gui §7- Ouvrir la liste des parties"));
        s.sendMessage(Msg.of("§e/cc hub §7- Définir le hub principal"));
        s.sendMessage(Msg.of("§e/cc <map> pos1|pos2|posconfirm §7- Définir la zone de jeu"));
        s.sendMessage(Msg.of("§e/cc <map> spawnseek §7- Définir le spawn du Seeker"));
        s.sendMessage(Msg.of("§e/cc <map> lobby §7- Définir le lobby d'attente"));
        s.sendMessage(Msg.of("§e/cc <map> time <ticks> §7- Durée de la partie"));
        s.sendMessage(Msg.of("§e/cc <map> killmax <n> §7- Coups max du Seeker"));
        s.sendMessage(Msg.of("§e/cc <map> maxplayers <n> §7- Joueurs max"));
        s.sendMessage(Msg.of("§e/cc <map> seeker <n> §7- Nombre de Seekers"));
        s.sendMessage(Msg.of("§e/cc <map> seeker virus §7- Active/désactive le mode virus"));
        s.sendMessage(Msg.of("§e/cc <map> mob <type> [%] §7- Ajouter un mob à la map"));
        s.sendMessage(Msg.of("§e/cc <map> listmob §7- Liste des mobs de la map"));
        s.sendMessage(Msg.of("§e/cc <map> hunt <tick> [nb] §7- Ajouter un déclenchement de hunt"));
        s.sendMessage(Msg.of("§e/cc <map> scenario §7- Ouvrir le GUI des scénarios"));
        s.sendMessage(Msg.of("§e/cc <map> save §7- Sauvegarder/valider la map"));
        s.sendMessage(Msg.of("§e/cc <map> config §7- Repasser la map en édition"));
    }

    // ------------------------------------------------------------- LIST

    private boolean handleList(CommandSender s) {
        Collection<GameMap> maps = plugin.getMapManager().getMaps();
        if (maps.isEmpty()) {
            s.sendMessage(Msg.of("§7Aucune map n'existe pour le moment."));
            return true;
        }
        s.sendMessage(Msg.of("§6Maps existantes:"));
        for (GameMap m : maps) {
            s.sendMessage(Msg.of(" §7- §f" + m.getName() + (m.isSaved() ? " §a[prête]" : " §e[en édition]")));
        }
        return true;
    }

    // ----------------------------------------------------------- CREATE

    private boolean handleCreate(CommandSender s, String[] args) {
        if (args.length < 2) { s.sendMessage(Msg.of("§cUsage: /cc create <nom>")); return true; }
        String name = args[1];
        if (reserved.contains(name.toLowerCase(Locale.ROOT))) {
            s.sendMessage(Msg.of("§cCe nom est réservé, choisissez-en un autre."));
            return true;
        }
        if (plugin.getMapManager().exists(name)) {
            s.sendMessage(Msg.of("§cUne map avec ce nom existe déjà."));
            return true;
        }
        plugin.getMapManager().createMap(name);
        s.sendMessage(Msg.of("§aMap §f" + name + " §acréée ! Configurez-la avec /cc " + name + " ..."));
        return true;
    }

    // ----------------------------------------------------------- DELETE

    private final Map<UUID, String> pendingDeletion = new HashMap<>();

    private boolean handleDelete(CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { s.sendMessage(Msg.of("§cCommande réservée aux joueurs.")); return true; }
        if (args.length < 2) { s.sendMessage(Msg.of("§cUsage: /cc delete <nom>")); return true; }
        String name = args[1];
        if (!plugin.getMapManager().exists(name)) {
            s.sendMessage(Msg.of("§cCette map n'existe pas."));
            return true;
        }
        String pending = pendingDeletion.get(p.getUniqueId());
        if (name.equalsIgnoreCase(pending)) {
            plugin.getMapManager().deleteMap(name);
            pendingDeletion.remove(p.getUniqueId());
            s.sendMessage(Msg.of("§aLa map §f" + name + " §aa été supprimée."));
        } else {
            pendingDeletion.put(p.getUniqueId(), name);
            s.sendMessage(Msg.of("§c⚠ Confirmez la suppression de §f" + name +
                    " §cen tapant à nouveau /cc delete " + name));
        }
        return true;
    }

    // ------------------------------------------------------------- HUB

    private boolean handleHub(CommandSender s) {
        if (!(s instanceof Player p)) { s.sendMessage(Msg.of("§cCommande réservée aux joueurs.")); return true; }
        plugin.getMapManager().setHub(p.getLocation());
        s.sendMessage(Msg.of("§aHub principal défini à votre position."));
        return true;
    }

    // ------------------------------------------------------------- GUI

    private boolean handleGui(CommandSender s) {
        if (!(s instanceof Player p)) { s.sendMessage(Msg.of("§cCommande réservée aux joueurs.")); return true; }
        p.openInventory(MapListGui.build(plugin));
        return true;
    }

    // -------------------------------------------------------- MAP EDIT

    private boolean handleMapSubcommand(CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { s.sendMessage(Msg.of("§cCommande réservée aux joueurs.")); return true; }
        String mapName = args[0];
        GameMap map = plugin.getMapManager().getMap(mapName);
        if (map == null) {
            s.sendMessage(Msg.of("§cCette map n'existe pas. Utilisez /cc create " + mapName));
            return true;
        }
        if (args.length < 2) {
            s.sendMessage(Msg.of("§cUtilisez /cc help pour voir les sous-commandes disponibles."));
            return true;
        }

        var session = plugin.getGameManager().getSession(mapName);
        boolean running = session != null && (session.getState().name().equals("RUNNING") || session.getState().name().equals("STARTING"));
        String sub = args[1].toLowerCase(Locale.ROOT);

        if (running && !sub.equals("scenario")) {
            s.sendMessage(Msg.of("§cImpossible de modifier une map dont la partie est en cours."));
            return true;
        }
        if (map.isSaved() && !sub.equals("config") && !sub.equals("scenario")) {
            s.sendMessage(Msg.of("§cCette map est sauvegardée. Utilisez /cc " + mapName + " config pour la modifier."));
            return true;
        }

        return switch (sub) {
            case "pos1" -> { map.setPos1(p.getLocation()); plugin.getMapManager().save(map); s.sendMessage(Msg.of("§aPos1 définie.")); yield true; }
            case "pos2" -> { map.setPos2(p.getLocation()); plugin.getMapManager().save(map); s.sendMessage(Msg.of("§aPos2 définie.")); yield true; }
            case "posconfirm" -> handlePosConfirm(s, map);
            case "spawnseek" -> { map.setSpawnSeeker(p.getLocation()); plugin.getMapManager().save(map); s.sendMessage(Msg.of("§aSpawn du Seeker défini.")); yield true; }
            case "lobby" -> { map.setLobby(p.getLocation()); plugin.getMapManager().save(map); s.sendMessage(Msg.of("§aLobby défini.")); yield true; }
            case "time" -> handleTime(s, map, args);
            case "killmax" -> handleKillmax(s, map, args);
            case "maxplayers" -> handleMaxPlayers(s, map, args);
            case "seeker" -> handleSeeker(s, map, args);
            case "mob" -> handleMob(s, map, args);
            case "listmob" -> handleListMob(s, map);
            case "hunt" -> handleHunt(s, map, args);
            case "scenario" -> { p.openInventory(ScenarioGui.build(map)); yield true; }
            case "save" -> handleSave(s, map);
            case "config" -> handleConfig(s, map);
            default -> { s.sendMessage(Msg.of("§cSous-commande inconnue.")); yield true; }
        };
    }

    private boolean handlePosConfirm(CommandSender s, GameMap map) {
        if (map.getPos1() == null || map.getPos2() == null) {
            s.sendMessage(Msg.of("§cDéfinissez pos1 et pos2 avant de confirmer."));
            return true;
        }
        map.confirmPos();
        plugin.getMapManager().save(map);
        s.sendMessage(Msg.of("§aZone de jeu confirmée."));
        return true;
    }

    private boolean handleTime(CommandSender s, GameMap map, String[] args) {
        if (args.length < 3) { s.sendMessage(Msg.of("§cUsage: /cc " + map.getName() + " time <ticks>")); return true; }
        try {
            int ticks = Integer.parseInt(args[2]);
            map.setTimeTicks(ticks);
            plugin.getMapManager().save(map);
            s.sendMessage(Msg.of("§aDurée définie: " + ticks + " ticks (~" + formatTicks(ticks) + ")"));
        } catch (NumberFormatException e) {
            s.sendMessage(Msg.of("§cValeur invalide."));
        }
        return true;
    }

    private String formatTicks(int ticks) {
        int totalSeconds = ticks / 20;
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        if (h > 0) return h + "h" + (m > 0 ? m + "min" : "");
        return m + " min";
    }

    private boolean handleKillmax(CommandSender s, GameMap map, String[] args) {
        if (args.length < 3) { s.sendMessage(Msg.of("§cUsage: /cc " + map.getName() + " killmax <n>")); return true; }
        try {
            map.setKillMax(Integer.parseInt(args[2]));
            plugin.getMapManager().save(map);
            s.sendMessage(Msg.of("§aKillmax défini: " + map.getKillMax()));
        } catch (NumberFormatException e) {
            s.sendMessage(Msg.of("§cValeur invalide."));
        }
        return true;
    }

    private boolean handleMaxPlayers(CommandSender s, GameMap map, String[] args) {
        if (args.length < 3) { s.sendMessage(Msg.of("§cUsage: /cc " + map.getName() + " maxplayers <n>")); return true; }
        try {
            map.setMaxPlayers(Integer.parseInt(args[2]));
            plugin.getMapManager().save(map);
            s.sendMessage(Msg.of("§aNombre de joueurs max défini: " + map.getMaxPlayers()));
        } catch (NumberFormatException e) {
            s.sendMessage(Msg.of("§cValeur invalide."));
        }
        return true;
    }

    private boolean handleSeeker(CommandSender s, GameMap map, String[] args) {
        if (args.length < 3) { s.sendMessage(Msg.of("§cUsage: /cc " + map.getName() + " seeker <n>|virus")); return true; }
        if (args[2].equalsIgnoreCase("virus")) {
            map.setVirusMode(!map.isVirusMode());
            plugin.getMapManager().save(map);
            s.sendMessage(Msg.of("§aMode virus " + (map.isVirusMode() ? "activé" : "désactivé") + "."));
            return true;
        }
        try {
            map.setSeekerCount(Math.max(1, Integer.parseInt(args[2])));
            plugin.getMapManager().save(map);
            s.sendMessage(Msg.of("§aNombre de Seekers défini: " + map.getSeekerCount()));
        } catch (NumberFormatException e) {
            s.sendMessage(Msg.of("§cValeur invalide."));
        }
        return true;
    }

    private boolean handleMob(CommandSender s, GameMap map, String[] args) {
        if (args.length < 3) { s.sendMessage(Msg.of("§cUsage: /cc " + map.getName() + " mob <type> [%]")); return true; }
        String mobName = args[2].toUpperCase(Locale.ROOT);
        EntityType type;
        try {
            type = EntityType.valueOf(mobName);
        } catch (IllegalArgumentException e) {
            s.sendMessage(Msg.of("§cType de mob invalide."));
            return true;
        }
        Integer percent = null;
        if (args.length >= 4) {
            try {
                percent = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                s.sendMessage(Msg.of("§cPourcentage invalide."));
                return true;
            }
        }
        map.getMobPercentages().put(type.name(), percent != null ? percent : 0);
        map.getMobExplicit().put(type.name(), percent != null);
        map.rebalanceMobPercentages();
        plugin.getMapManager().save(map);
        s.sendMessage(Msg.of("§aMob §f" + type.name() + " §aajouté/mis à jour."));
        return true;
    }

    private boolean handleListMob(CommandSender s, GameMap map) {
        if (map.getMobPercentages().isEmpty()) {
            s.sendMessage(Msg.of("§7Aucun mob défini sur cette map."));
            return true;
        }
        s.sendMessage(Msg.of("§6Mobs de la map " + map.getName() + ":"));
        for (var e : map.getMobPercentages().entrySet()) {
            s.sendMessage(Msg.of(" §7- §f" + e.getKey() + " §7: §e" + e.getValue() + "%"));
        }
        return true;
    }

    private boolean handleHunt(CommandSender s, GameMap map, String[] args) {
        if (args.length < 3) { s.sendMessage(Msg.of("§cUsage: /cc " + map.getName() + " hunt <tick> [nombre]")); return true; }
        try {
            int tick = Integer.parseInt(args[2]);
            if (map.getTimeTicks() > 0 && tick > map.getTimeTicks()) {
                s.sendMessage(Msg.of("§cCe déclenchement dépasse la durée totale de la partie."));
                return true;
            }
            int count = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
            map.getHunts().add(new GameMap.HuntEntry(tick, count));
            plugin.getMapManager().save(map);
            s.sendMessage(Msg.of("§aHunt ajouté au tick " + tick + " (" + count + " feu(x) d'artifice)."));
        } catch (NumberFormatException e) {
            s.sendMessage(Msg.of("§cValeur invalide."));
        }
        return true;
    }

    private boolean handleSave(CommandSender s, GameMap map) {
        List<String> missing = map.getMissingRequirements();
        if (!missing.isEmpty()) {
            s.sendMessage(Msg.of("§cImpossible de sauvegarder, il manque: §f" + String.join(", ", missing)));
            return true;
        }
        map.setSaved(true);
        map.setInMaintenance(false);
        plugin.getMapManager().save(map);
        s.sendMessage(Msg.of("§aMap §f" + map.getName() + " §asauvegardée et disponible dans /cc gui !"));
        return true;
    }

    private boolean handleConfig(CommandSender s, GameMap map) {
        map.setInMaintenance(true);
        plugin.getMapManager().save(map);
        s.sendMessage(Msg.of("§eLa map §f" + map.getName() + " §eest en édition. Refaites /cc " + map.getName() + " save pour valider."));
        return true;
    }

    // -------------------------------------------------------- TAB COMPLETE

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("create", "delete", "list", "help", "hub", "gui"));
            options.addAll(plugin.getMapManager().getMaps().stream().map(GameMap::getName).toList());
            return filter(options, args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("delete")) {
                return filter(plugin.getMapManager().getMaps().stream().map(GameMap::getName).toList(), args[1]);
            }
            if (plugin.getMapManager().exists(args[0])) {
                return filter(List.of("pos1", "pos2", "posconfirm", "spawnseek", "lobby", "time", "killmax",
                        "maxplayers", "seeker", "mob", "listmob", "hunt", "scenario", "save", "config"), args[1]);
            }
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("mob")) {
            return filter(Arrays.stream(EntityType.values()).map(Enum::name).toList(), args[2]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("seeker")) {
            return filter(List.of("virus", "1", "2", "3"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }
}
