package com.hideseek.cachecache.game;

import com.hideseek.cachecache.util.Msg;

import com.hideseek.cachecache.CacheCachePlugin;
import com.hideseek.cachecache.map.GameMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

public class GameManager {

    private final CacheCachePlugin plugin;
    private final Map<String, GameSession> sessions = new LinkedHashMap<>();
    private final Random random = new Random();

    public GameManager(CacheCachePlugin plugin) {
        this.plugin = plugin;
    }

    public GameSession getOrCreateSession(GameMap map) {
        return sessions.computeIfAbsent(map.getName().toLowerCase(Locale.ROOT), k -> {
            GameSession s = new GameSession(plugin, map);
            s.startTicking();
            return s;
        });
    }

    public GameSession getSession(String mapName) {
        return sessions.get(mapName.toLowerCase(Locale.ROOT));
    }

    public boolean joinGame(Player p, GameMap map) {
        if (!map.isSaved()) {
            p.sendMessage(net.kyori.adventure.text.Msg.of("§cCette map n'est pas encore prête."));
            return false;
        }
        GameSession session = getOrCreateSession(map);
        if (session.getState() == GameState.RUNNING || session.getState() == GameState.STARTING) {
            joinAsSpectator(p, session);
            return true;
        }
        return session.addToLobby(p);
    }

    public void joinAsSpectator(Player p, GameSession session) {
        session.getSpectators().add(p.getUniqueId());
        p.teleport(session.getMap().getPos1() != null ? session.getMap().getPos1() : p.getLocation());
        p.setGameMode(org.bukkit.GameMode.SPECTATOR);
        p.sendMessage(net.kyori.adventure.text.Msg.of("§7Vous observez la partie en cours."));
    }

    public void quitToHub(Player p) {
        for (GameSession s : sessions.values()) {
            s.getLobbyPlayers().remove(p.getUniqueId());
            s.getSpectators().remove(p.getUniqueId());
            if (s.getAliveHidden().remove(p.getUniqueId()) || s.getSeekers().remove(p.getUniqueId())) {
                plugin.getDisguiseManager().undisguise(p);
            }
        }
        Location hub = plugin.getMapManager().getHub();
        if (hub != null) p.teleport(hub);
        p.setGameMode(org.bukkit.GameMode.ADVENTURE);
        p.getInventory().clear();
        p.setWalkSpeed(0.2f);
    }

    public void resetSession(GameSession session) {
        for (Player p : session.getAllOnlinePlayers()) {
            quitToHub(p);
        }
        session.getLobbyPlayers().clear();
        session.getAliveHidden().clear();
        session.getSeekers().clear();
        session.getSpectators().clear();
        session.setState(GameState.LOBBY);
    }

    public Collection<GameSession> getAllSessions() { return sessions.values(); }

    /**
     * Cherche un emplacement aléatoire au sol (bloc plein en dessous, air au-dessus),
     * en évitant les 5 blocs les plus hauts de la zone et les blocs invisibles (BARRIER, LIGHT, STRUCTURE_VOID).
     */
    public Location findRandomGroundLocation(GameMap map) {
        Location p1 = map.getPos1();
        Location p2 = map.getPos2();
        if (p1 == null || p2 == null) return null;
        World world = p1.getWorld();

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int excludedTop = Math.max(minY, maxY - 5);

        for (int attempt = 0; attempt < 60; attempt++) {
            int x = minX + random.nextInt(Math.max(1, maxX - minX + 1));
            int z = minZ + random.nextInt(Math.max(1, maxZ - minZ + 1));
            for (int y = excludedTop; y >= minY; y--) {
                Block ground = world.getBlockAt(x, y, z);
                Block above = world.getBlockAt(x, y + 1, z);
                Block above2 = world.getBlockAt(x, y + 2, z);
                if (isInvalidGround(ground)) continue;
                if (ground.getType().isSolid() && above.getType().isAir() && above2.getType().isAir()) {
                    return new Location(world, x + 0.5, y + 1, z + 0.5);
                }
            }
        }
        return null;
    }

    private boolean isInvalidGround(Block block) {
        switch (block.getType()) {
            case BARRIER:
            case LIGHT:
            case STRUCTURE_VOID:
            case AIR:
            case VOID_AIR:
            case CAVE_AIR:
                return true;
            default:
                return false;
        }
    }
}
