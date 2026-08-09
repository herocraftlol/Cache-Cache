package com.hideseek.cachecache.disguise;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.hideseek.cachecache.CacheCachePlugin;
import com.hideseek.cachecache.game.GameSession;
import com.hideseek.cachecache.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Camouflage 100% par paquets : aucune vraie entité n'existe côté serveur, donc AUCUNE
 * collision ni physique n'est possible — c'est purement un rendu visuel envoyé aux
 * clients. Le vrai joueur reste invisible (potion d'Invisibilité, gérée par
 * {@link DisguiseManager}) ; ce backend s'occupe uniquement du mob visuel qui le suit.
 *
 * Comme il n'y a pas de vraie entité, le combat ne peut pas passer par les événements
 * Bukkit habituels : on intercepte directement le paquet client "USE_ENTITY" (attaque/clic)
 * envoyé par le Seeker, on vérifie s'il vise l'ID fictif d'un joueur caché, et on applique
 * nous-mêmes la logique d'élimination.
 *
 * ATTENTION : ce backend dépend fortement des classes wrapper générées par ProtocolLib,
 * qui peuvent changer de signature d'une version à l'autre. Il a été écrit avec le plus
 * grand soin mais n'a pas pu être compilé/testé contre un vrai serveur dans cet
 * environnement (pas d'accès réseau au dépôt de ProtocolLib). Si tu rencontres une erreur
 * de compilation ou un comportement anormal, montre-la moi et je corrige précisément.
 */
public class PacketDisguiseBackend {

    private final CacheCachePlugin plugin;
    private final ProtocolManager protocolManager;

    private final Map<UUID, Integer> playerToFakeId = new HashMap<>();
    private final Map<Integer, UUID> fakeIdToPlayer = new HashMap<>();
    private final Map<UUID, UUID> fakeEntityUuids = new HashMap<>();

    private final AtomicInteger idCounter = new AtomicInteger(2_000_000_000);

    public PacketDisguiseBackend(CacheCachePlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void start() {
        registerCombatListener();
        Bukkit.getScheduler().runTaskTimer(plugin, this::syncAll, 1L, 1L);
    }

    public void stop() {
        for (UUID playerId : new java.util.ArrayList<>(playerToFakeId.keySet())) {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null) undisguise(p);
        }
        playerToFakeId.clear();
        fakeIdToPlayer.clear();
        fakeEntityUuids.clear();
    }

    public void disguise(Player player, EntityType type) {
        undisguise(player);

        int fakeId = idCounter.decrementAndGet();
        UUID fakeUuid = UUID.randomUUID();
        Location loc = player.getLocation();

        try {
            PacketContainer spawn = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawn.getIntegers().write(0, fakeId);
            spawn.getUUIDs().write(0, fakeUuid);
            spawn.getEntityTypeModifier().write(0, type);
            spawn.getDoubles().write(0, loc.getX());
            spawn.getDoubles().write(1, loc.getY());
            spawn.getDoubles().write(2, loc.getZ());
            spawn.getBytes().writeSafely(0, (byte) (loc.getYaw() * 256.0F / 360.0F));
            spawn.getBytes().writeSafely(1, (byte) (loc.getPitch() * 256.0F / 360.0F));

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                protocolManager.sendServerPacket(viewer, spawn, false);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Échec de la création de l'entité fictive via ProtocolLib (" + t.getMessage()
                    + "). Le déguisement par paquets est annulé pour ce joueur ; vérifie ta version de ProtocolLib.");
            return;
        }

        playerToFakeId.put(player.getUniqueId(), fakeId);
        fakeIdToPlayer.put(fakeId, player.getUniqueId());
        fakeEntityUuids.put(player.getUniqueId(), fakeUuid);
    }

    public void undisguise(Player player) {
        Integer fakeId = playerToFakeId.remove(player.getUniqueId());
        fakeEntityUuids.remove(player.getUniqueId());
        if (fakeId == null) return;
        fakeIdToPlayer.remove(fakeId);

        try {
            PacketContainer destroy = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroy.getIntegerArrays().write(0, new int[]{fakeId});
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                protocolManager.sendServerPacket(viewer, destroy, false);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Échec de la suppression de l'entité fictive via ProtocolLib : " + t.getMessage());
        }
    }

    public boolean isDisguised(Player player) {
        return playerToFakeId.containsKey(player.getUniqueId());
    }

    private void syncAll() {
        if (playerToFakeId.isEmpty()) return;
        for (Map.Entry<UUID, Integer> entry : new HashMap<>(playerToFakeId).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            int fakeId = entry.getValue();
            Location loc = player.getLocation();

            try {
                PacketContainer teleport = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
                teleport.getIntegers().write(0, fakeId);
                teleport.getDoubles().write(0, loc.getX());
                teleport.getDoubles().write(1, loc.getY());
                teleport.getDoubles().write(2, loc.getZ());
                teleport.getBytes().writeSafely(0, (byte) (loc.getYaw() * 256.0F / 360.0F));
                teleport.getBytes().writeSafely(1, (byte) (loc.getPitch() * 256.0F / 360.0F));
                teleport.getBooleans().writeSafely(0, true);

                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    protocolManager.sendServerPacket(viewer, teleport, false);
                }
            } catch (Throwable ignored) {
                // Si un paquet échoue ponctuellement, on retentera au tick suivant.
            }
        }
    }

    /**
     * Intercepte le clic/attaque envoyé par le client sur l'entité fictive, et applique
     * nous-mêmes la logique d'élimination (puisqu'aucun événement Bukkit ne se déclenche
     * pour une entité qui n'existe pas vraiment côté serveur).
     */
    private void registerCombatListener() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                int targetId;
                try {
                    targetId = event.getPacket().getIntegers().read(0);
                } catch (Throwable t) {
                    return;
                }
                UUID hiddenId = fakeIdToPlayer.get(targetId);
                if (hiddenId == null) return;

                Player attacker = event.getPlayer();
                // Traité comme une attaque : ces entités fictives n'ont aucune interaction
                // légitime autre que le combat, donc tout clic dessus vaut une attaque.
                Bukkit.getScheduler().runTask(plugin, () -> handleAttackOnFake(attacker, hiddenId));
            }
        });
    }

    private void handleAttackOnFake(Player attacker, UUID hiddenId) {
        for (GameSession session : plugin.getGameManager().getAllSessions()) {
            if (session.getState() != GameState.RUNNING) continue;
            if (!session.isSeeker(attacker.getUniqueId())) continue;
            if (!session.isAliveHidden(hiddenId)) continue;

            if (!session.seekerHasKillsLeft(attacker.getUniqueId())) {
                attacker.sendMessage(com.hideseek.cachecache.util.Msg.of("§cVous n'avez plus de coups disponibles !"));
                return;
            }

            Player victim = Bukkit.getPlayer(hiddenId);
            if (victim == null) return;

            session.registerSeekerKill(attacker.getUniqueId());
            session.onHiddenEliminated(victim);
            return;
        }
    }
}
