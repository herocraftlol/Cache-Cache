package com.hideseek.cachecache.listeners;

import com.hideseek.cachecache.util.Msg;

import com.hideseek.cachecache.CacheCachePlugin;
import com.hideseek.cachecache.game.GameSession;
import com.hideseek.cachecache.game.GameState;
import com.hideseek.cachecache.map.GameMap;
import com.hideseek.cachecache.map.Scenario;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final CacheCachePlugin plugin;

    public PlayerListener(CacheCachePlugin plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Long> lastArenaWarning = new java.util.HashMap<>();

    private GameSession sessionOf(Player p) {
        for (GameSession s : plugin.getGameManager().getAllSessions()) {
            if (s.getSeekers().contains(p.getUniqueId())
                    || s.getAliveHidden().contains(p.getUniqueId())
                    || s.getSpectators().contains(p.getUniqueId())
                    || s.getLobbyPlayers().contains(p.getUniqueId())) {
                return s;
            }
        }
        return null;
    }

    // -------------------------------------------------------------- DAMAGE

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        GameSession session = sessionOf(victim);
        if (session == null || session.getState() != GameState.RUNNING) {
            e.setCancelled(true);
            return;
        }

        UUID victimId = victim.getUniqueId();

        // Dégâts par flèche (scénario Arc)
        if (e.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player shooter) {
            handleAttack(session, e, shooter, victim);
            return;
        }

        if (!(e.getDamager() instanceof Player attacker)) {
            e.setCancelled(true);
            return;
        }

        handleAttack(session, e, attacker, victim);
    }

    private void handleAttack(GameSession session, EntityDamageEvent e, Player attacker, Player victim) {
        boolean attackerIsSeeker = session.isSeeker(attacker.getUniqueId());
        boolean victimIsSeeker = session.isSeeker(victim.getUniqueId());

        if (attackerIsSeeker && !victimIsSeeker) {
            if (!session.seekerHasKillsLeft(attacker.getUniqueId())) {
                e.setCancelled(true);
                attacker.sendMessage(Msg.of("§cVous n'avez plus de coups disponibles !"));
                return;
            }
            e.setDamage(1000);
            session.registerSeekerKill(attacker.getUniqueId());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (session.isAliveHidden(victim.getUniqueId())) session.onHiddenEliminated(victim);
            });
        } else if (!attackerIsSeeker && victimIsSeeker) {
            if (session.hasScenario(Scenario.TROLL_SWORD)) {
                e.setDamage(0);
            } else {
                e.setCancelled(true);
            }
        } else {
            e.setCancelled(true);
        }
    }

    /**
     * Le Seeker one-shot instantanément n'importe quel mob (décor ou naturel) qu'il frappe,
     * exactement comme s'il touchait un joueur caché — puisqu'il ne peut pas savoir lequel
     * est réel. Ça consomme aussi un coup de son quota (killmax), donc s'acharner sur les
     * décors sans discernement peut lui coûter la partie.
     */
    @EventHandler
    public void onSeekerHitsMob(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player) return; // les joueurs sont gérés par onDamage
        if (!(e.getEntity() instanceof LivingEntity mob)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;

        GameSession session = sessionOf(attacker);
        if (session == null || session.getState() != GameState.RUNNING) return;
        if (!session.isSeeker(attacker.getUniqueId())) return;

        e.setCancelled(true);

        if (!session.seekerHasKillsLeft(attacker.getUniqueId())) {
            attacker.sendMessage(Msg.of("§cVous n'avez plus de coups disponibles !"));
            return;
        }

        session.registerSeekerKill(attacker.getUniqueId());
        mob.setHealth(0);
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
    }

    @EventHandler
    public void onGenericDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        GameSession session = sessionOf(p);
        if (session == null) return;
        if (session.getState() != GameState.RUNNING) {
            e.setCancelled(true);
            return;
        }
        // La map ne peut jamais être dégradée : on annule tout dégât d'explosion sur les blocs plus tard (BlockListener)
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && session.isAliveHidden(p.getUniqueId())) {
            e.setCancelled(true); // évite de perdre des joueurs cachés à cause d'une chute de spawn
        }
    }

    // Scénario 8 : les mobs hostiles attaquent (ou non) le Seeker
    @EventHandler
    public void onTarget(EntityTargetEvent e) {
        if (!(e.getTarget() instanceof Player target)) return;
        if (!(e.getEntity() instanceof Monster)) return;
        GameSession session = sessionOf(target);
        if (session == null || session.getState() != GameState.RUNNING) return;
        boolean isSeeker = session.isSeeker(target.getUniqueId());
        if (isSeeker && !session.hasScenario(Scenario.HOSTILE_MOBS)) {
            e.setCancelled(true);
        } else if (!isSeeker) {
            e.setCancelled(true);
        }
    }

    // ----------------------------------------------------------- INTERACT

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta()) return;
        String tag = item.getItemMeta().getPersistentDataContainer()
                .get(GameSession.KEY_ITEM, PersistentDataType.STRING);
        if (tag == null) return;

        Player p = e.getPlayer();
        GameSession session = sessionOf(p);
        if (session == null) return;

        switch (tag) {
            case "leave_watch" -> {
                e.setCancelled(true);
                plugin.getGameManager().quitToHub(p);
            }
            case "hunt_firework" -> {
                e.setCancelled(true);
                handleHuntFirework(session, p, item);
            }
            case "anvil_power" -> {
                e.setCancelled(true);
                handleAnvilPower(session, p);
            }
            case "tnt_launch" -> {
                e.setCancelled(true);
                handleTntLaunch(session, p, item);
            }
            case "blaze_lightning" -> {
                e.setCancelled(true);
                handleBlazeLightning(session, p);
            }
        }
    }

    private void handleHuntFirework(GameSession session, Player p, ItemStack item) {
        if (session.getState() != GameState.RUNNING) return;
        for (UUID seekerId : session.getSeekers()) {
            Player seeker = Bukkit.getPlayer(seekerId);
            if (seeker == null) continue;
            seeker.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, seeker.getLocation(), 40, 0.5, 1, 0.5, 0.05);
            seeker.getWorld().playSound(seeker.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        if (!session.hasScenario(Scenario.HUNT_INFINITE)) {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private void handleAnvilPower(GameSession session, Player p) {
        if (session.getState() != GameState.RUNNING || !session.isSeeker(p.getUniqueId())) return;
        if (!session.tryUseAnvil(p.getUniqueId())) {
            p.sendMessage(Msg.of("§cCe pouvoir est encore en recharge !"));
            return;
        }
        for (UUID hiddenId : session.getAliveHidden()) {
            Player hidden = Bukkit.getPlayer(hiddenId);
            if (hidden == null) continue;
            Location above = hidden.getLocation().clone().add(0, 6, 0);
            FallingBlock fb = hidden.getWorld().spawnFallingBlock(above, Material.ANVIL.createBlockData());
            fb.setDropItem(false);
            fb.setHurtEntities(false); // effet troll uniquement, pas d'élimination via l'enclume
        }
    }

    private void handleTntLaunch(GameSession session, Player p, ItemStack item) {
        if (session.getState() != GameState.RUNNING) return;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize().multiply(1.2);
        TNTPrimed tnt = p.getWorld().spawn(eye.clone().add(dir), TNTPrimed.class);
        tnt.setFuseTicks(60);
        tnt.setVelocity(dir);
        tnt.setYield(0f); // aucune dégradation de la map
        tnt.setIsIncendiary(false);
        tnt.setMetadata("cc_no_damage_tnt", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
    }

    private void handleBlazeLightning(GameSession session, Player p) {
        if (session.getState() != GameState.RUNNING) return;
        if (!session.tryUseBlazeRod(p.getUniqueId())) {
            p.sendMessage(Msg.of("§cCe pouvoir est encore en recharge !"));
            return;
        }
        var target = p.getTargetBlockExact(200);
        if (target == null) return;
        p.getWorld().strikeLightningEffect(target.getLocation().add(0.5, 1, 0.5)); // effet visuel uniquement, aucun dégât
    }

    // -------------------------------------------------------------- MOVE

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        GameSession session = sessionOf(p);
        if (session == null) return;
        Location to = e.getTo();
        if (to == null) return;

        // Joueurs en attente dans le lobby : confinés à la plateforme 8x8
        if (session.getLobbyPlayers().contains(p.getUniqueId())) {
            Location lobby = session.getMap().getLobby();
            if (lobby == null || to.getWorld() == null || !to.getWorld().equals(lobby.getWorld())) return;
            int half = com.hideseek.cachecache.map.GameMap.LOBBY_PLATFORM_SIZE / 2;
            int minX = lobby.getBlockX() - half;
            int maxX = lobby.getBlockX() + half - 1;
            int minZ = lobby.getBlockZ() - half;
            int maxZ = lobby.getBlockZ() + half - 1;
            if (to.getBlockX() < minX || to.getBlockX() > maxX || to.getBlockZ() < minZ || to.getBlockZ() > maxZ) {
                Location clamped = e.getFrom().clone();
                clamped.setPitch(to.getPitch());
                clamped.setYaw(to.getYaw());
                e.setTo(clamped);
            }
            return;
        }

        // Spectateurs (cachés éliminés, Seekers en virus déjà gérés à part, ou observateurs) :
        // confinés strictement à la zone de l'arène, renvoyés au centre s'ils atteignent le bord.
        if (!session.isSpectator(p.getUniqueId())) return;

        GameMap map = session.getMap();
        if (map.getPos1() == null || map.getPos2() == null) return;
        if (to.getWorld() == null || !to.getWorld().equals(map.getPos1().getWorld())) return;

        int minX = Math.min(map.getPos1().getBlockX(), map.getPos2().getBlockX());
        int maxX = Math.max(map.getPos1().getBlockX(), map.getPos2().getBlockX());
        int minZ = Math.min(map.getPos1().getBlockZ(), map.getPos2().getBlockZ());
        int maxZ = Math.max(map.getPos1().getBlockZ(), map.getPos2().getBlockZ());
        int minY = Math.min(map.getPos1().getBlockY(), map.getPos2().getBlockY()) - 5;
        int maxY = Math.max(map.getPos1().getBlockY(), map.getPos2().getBlockY()) + 40;

        if (to.getBlockX() <= minX || to.getBlockX() >= maxX
                || to.getBlockZ() <= minZ || to.getBlockZ() >= maxZ
                || to.getBlockY() < minY || to.getBlockY() > maxY) {
            Location center = plugin.getGameManager().getArenaCenter(map);
            if (center != null) {
                center.setPitch(to.getPitch());
                center.setYaw(to.getYaw());
                p.teleport(center);
                long now = System.currentTimeMillis();
                if (now - lastArenaWarning.getOrDefault(p.getUniqueId(), 0L) > 3000L) {
                    lastArenaWarning.put(p.getUniqueId(), now);
                    p.sendMessage(Msg.of("§cVous ne pouvez pas quitter l'arène en spectateur."));
                }
            }

        }
    }

    // -------------------------------------------------------------- QUIT

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        GameSession session = sessionOf(p);
        if (session == null) return;

        if (session.getAliveHidden().remove(p.getUniqueId())) {
            plugin.getDisguiseManager().undisguise(p);
        }
        session.getSeekers().remove(p.getUniqueId());
        session.getLobbyPlayers().remove(p.getUniqueId());
        session.getSpectators().remove(p.getUniqueId());
    }
}
