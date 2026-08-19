package com.hideseek.cachecache.disguise;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketContainer;
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
 * Les paquets sont construits directement en {@link PacketContainer} (API stable de
 * ProtocolLib 5.x), sans les classes wrapper optionnelles, pour que le projet compile
 * quelle que soit la distribution exacte de ProtocolLib installée à la compilation. En
 * cas d'échec d'un envoi (champs de paquet différents selon la version de Minecraft), le
 * plugin retombe automatiquement sur le système natif : il ne plante jamais la partie.
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
        sendDestroy(fakeId);
    }

    /**
     * Envoie le paquet de suppression de l'entité fictive. Le champ exact de ce paquet a
     * changé de type selon les versions de Minecraft/ProtocolLib (tableau d'entiers puis
     * liste d'entiers) : on tente plusieurs méthodes dans l'ordre jusqu'à ce que l'une
     * fonctionne, pour rester robuste face à cette variation.
     */
    private void sendDestroy(int fakeId) {
        // Tentative 1 : paquet brut, champ "liste d'entiers" (versions récentes de Minecraft).
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            packet.getIntLists().write(0, java.util.List.of(fakeId));
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                protocolManager.sendServerPacket(viewer, packet, false);
            }
            return;
        } catch (Throwable ignored) {
            // On retente avec un tableau d'entiers ci-dessous.
        }

        // Tentative 2 : paquet brut, champ "tableau d'entiers" (anciennes versions).
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            packet.getIntegerArrays().write(0, new int[]{fakeId});
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                protocolManager.sendServerPacket(viewer, packet);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Échec de la suppression de l'entité fictive (toutes les méthodes ont échoué) : "
                    + t.getMessage() + ". L'entité fantôme risque de rester visible ; contacte-moi avec cette erreur.");
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

            // Le paquet de téléportation ne contrôle que le corps : sans ce paquet dédié,
            // la tête du mob reste figée dans sa direction d'origine et ne suit jamais le
            // regard du joueur.
            try {
                PacketContainer headRotation = protocolManager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
                headRotation.getIntegers().write(0, fakeId);
                headRotation.getBytes().writeSafely(0, (byte) (loc.getYaw() * 256.0F / 360.0F));

                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    protocolManager.sendServerPacket(viewer, headRotation, false);
                }
            } catch (Throwable ignored) {
                // Idem, on retentera au tick suivant.
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
