package com.hideseek.cachecache.game;

import com.hideseek.cachecache.util.Msg;

import com.hideseek.cachecache.CacheCachePlugin;
import com.hideseek.cachecache.map.GameMap;
import com.hideseek.cachecache.map.Scenario;
import com.hideseek.cachecache.scoreboard.ScoreboardHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class GameSession {

    public static final NamespacedKey KEY_ITEM = new NamespacedKey("cachecache", "special_item");

    private final CacheCachePlugin plugin;
    private final GameMap map;
    private GameState state = GameState.LOBBY;

    private final Set<UUID> lobbyPlayers = new LinkedHashSet<>();
    private final Set<UUID> aliveHidden = new LinkedHashSet<>();
    private final Set<UUID> seekers = new LinkedHashSet<>();
    private final Set<UUID> spectators = new LinkedHashSet<>();
    private final Map<UUID, EntityType> disguises = new HashMap<>();
    private final Map<UUID, Integer> killsUsed = new HashMap<>();
    private final Set<Scenario> activeScenarios = EnumSet.noneOf(Scenario.class);
    private final List<GameMap.HuntEntry> pendingHunts = new ArrayList<>();
    private final Map<UUID, Long> anvilCooldown = new HashMap<>();
    private final Map<UUID, Long> blazeCooldown = new HashMap<>();

    private final Random random = new Random();
    private final ScoreboardHandler scoreboardHandler;
    private BukkitTask tickTask;

    private int countdownSeconds = 15;
    private int startingTicksLeft = 300; // 15s à 20 tick/s
    private int ticksLeft;
    private boolean timeIndefinite;
    private boolean mobSwapDone = false;
    private int mobSwapTriggerTick = -1;
    private int elapsedTicks = 0;

    public GameSession(CacheCachePlugin plugin, GameMap map) {
        this.plugin = plugin;
        this.map = map;
        this.scoreboardHandler = new ScoreboardHandler(this);
    }

    public GameMap getMap() { return map; }
    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }

    public Set<UUID> getLobbyPlayers() { return lobbyPlayers; }
    public Set<UUID> getAliveHidden() { return aliveHidden; }
    public Set<UUID> getSeekers() { return seekers; }
    public Set<UUID> getSpectators() { return spectators; }

    public boolean isSeeker(UUID uuid) { return seekers.contains(uuid); }
    public boolean isAliveHidden(UUID uuid) { return aliveHidden.contains(uuid); }
    public boolean isSpectator(UUID uuid) { return spectators.contains(uuid); }
    public boolean hasScenario(Scenario s) { return activeScenarios.contains(s); }
    public int getKillsUsed(UUID uuid) { return killsUsed.getOrDefault(uuid, 0); }
    public int getAlivePlayersCount() { return aliveHidden.size(); }

    public List<Player> getAllOnlinePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID id : combinedIds()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) list.add(p);
        }
        return list;
    }

    private Set<UUID> combinedIds() {
        Set<UUID> all = new LinkedHashSet<>();
        all.addAll(lobbyPlayers);
        all.addAll(aliveHidden);
        all.addAll(seekers);
        all.addAll(spectators);
        return all;
    }

    // ---------------------------------------------------------------- LOBBY

    public boolean addToLobby(Player p) {
        if (state != GameState.LOBBY && state != GameState.COUNTDOWN) return false;
        if (map.getMaxPlayers() > 0 && lobbyPlayers.size() >= map.getMaxPlayers()) return false;
        lobbyPlayers.add(p.getUniqueId());
        teleportToLobby(p);
        broadcastLobby(Msg.of("§e" + p.getName() + " §7a rejoint la partie (" + lobbyPlayers.size() + "/" + map.getMaxPlayers() + ")"));
        checkAutoStart();
        return true;
    }

    public void removeFromLobby(Player p) {
        lobbyPlayers.remove(p.getUniqueId());
        if (lobbyPlayers.size() < 2 && state == GameState.COUNTDOWN) {
            cancelCountdown();
        }
    }

    private void teleportToLobby(Player p) {
        Location loc = map.getLobby().clone().add(0.5, 1.0, 0.5);
        p.teleport(loc);
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
    }

    private void broadcastLobby(Component msg) {
        for (UUID id : lobbyPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(msg);
        }
    }

    private void checkAutoStart() {
        if (state == GameState.LOBBY && lobbyPlayers.size() >= 2) {
            state = GameState.COUNTDOWN;
            countdownSeconds = 15;
        }
        if (map.getMaxPlayers() > 0 && lobbyPlayers.size() >= map.getMaxPlayers()) {
            countdownSeconds = Math.min(countdownSeconds, 5);
        }
    }

    private void cancelCountdown() {
        state = GameState.LOBBY;
        countdownSeconds = 15;
        broadcastLobby(Msg.of("§cPas assez de joueurs, compte à rebours annulé."));
    }

    // ------------------------------------------------------------- TICKING

    public void startTicking() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 1L);
    }

    public void stopTicking() {
        if (tickTask != null) tickTask.cancel();
    }

    private int scoreboardTickCounter = 0;

    private void tick() {
        switch (state) {
            case COUNTDOWN -> tickCountdown();
            case STARTING -> tickStarting();
            case RUNNING -> tickRunning();
            default -> {}
        }
        scoreboardTickCounter++;
        if (scoreboardTickCounter >= 20 && (state == GameState.STARTING || state == GameState.RUNNING)) {
            scoreboardHandler.update();
            scoreboardTickCounter = 0;
        }
    }

    private int countdownTickAccum = 0;

    private void tickCountdown() {
        countdownTickAccum++;
        if (countdownTickAccum < 20) return;
        countdownTickAccum = 0;
        countdownSeconds--;
        if (countdownSeconds <= 0) {
            beginStartingPhase();
        } else if (countdownSeconds <= 5 || countdownSeconds % 5 == 0) {
            broadcastLobby(Msg.of("§eLa partie commence dans §f" + countdownSeconds + "s"));
        }
    }

    private void tickStarting() {
        startingTicksLeft--;
        for (UUID id : seekers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.setWalkSpeed(0f);
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, false, false));
            }
        }
        if (startingTicksLeft <= 0) {
            for (UUID id : seekers) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    p.setWalkSpeed(0.2f);
                    p.removePotionEffect(PotionEffectType.BLINDNESS);
                }
            }
            state = GameState.RUNNING;
            broadcastAll(Msg.of("§c§lLA CHASSE COMMENCE !"));
        }
    }

    private void tickRunning() {
        elapsedTicks++;

        if (!timeIndefinite && !hasScenario(Scenario.INFINITE_TIME)) {
            ticksLeft--;
            if (ticksLeft <= 0) {
                endGame(true, "§bLe temps est écoulé !");
                return;
            }
        }

        // Hunts programmés
        Iterator<GameMap.HuntEntry> it = pendingHunts.iterator();
        while (it.hasNext()) {
            GameMap.HuntEntry h = it.next();
            if (elapsedTicks >= h.triggerTick) {
                giveHuntFirework(h.fireworkCount);
                it.remove();
            }
        }

        // Charge de vent : changement de mob unique à un moment aléatoire
        if (hasScenario(Scenario.MOB_SWAP) && !mobSwapDone) {
            if (mobSwapTriggerTick < 0) {
                int totalTime = map.getTimeTicks() > 0 ? map.getTimeTicks() : 12000;
                mobSwapTriggerTick = 20 + random.nextInt(Math.max(1, totalTime - 40));
            }
            if (elapsedTicks >= mobSwapTriggerTick) {
                performMobSwap();
                mobSwapDone = true;
            }
        }

        checkWinConditions();
    }

    // --------------------------------------------------------------- START

    public void beginStartingPhase() {
        state = GameState.STARTING;
        startingTicksLeft = 300;
        elapsedTicks = 0;
        ticksLeft = map.getTimeTicks();
        timeIndefinite = false;
        mobSwapDone = false;
        mobSwapTriggerTick = -1;
        activeScenarios.clear();
        activeScenarios.addAll(map.getScenarios());
        killsUsed.clear();
        pendingHunts.clear();
        pendingHunts.addAll(map.getHunts());
        pendingHunts.sort(Comparator.comparingInt(h -> h.triggerTick));

        List<UUID> players = new ArrayList<>(lobbyPlayers);
        Collections.shuffle(players, random);

        int seekerCount = Math.max(1, Math.min(map.getSeekerCount(), players.size() - 1));
        for (int i = 0; i < seekerCount; i++) {
            seekers.add(players.get(i));
        }
        for (int i = seekerCount; i < players.size(); i++) {
            aliveHidden.add(players.get(i));
        }
        lobbyPlayers.clear();

        List<EntityType> mobPool = buildWeightedMobPool();

        for (UUID id : aliveHidden) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            EntityType type = mobPool.isEmpty() ? EntityType.PIG : mobPool.get(random.nextInt(mobPool.size()));
            disguises.put(id, type);
            plugin.getDisguiseManager().disguiseAsMob(p, type);
            teleportRandomInRegion(p);
            p.setGameMode(GameMode.SURVIVAL);
        }

        for (UUID id : seekers) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            p.teleport(map.getSpawnSeeker());
            p.setGameMode(GameMode.SURVIVAL);
            equipSeeker(p);
            p.sendMessage(Msg.of("§c§lVous êtes le SEEKER ! Trouvez et éliminez tout le monde."));
        }

        giveHiddenScenarioItems();
        broadcastAll(Msg.of("§7Le Seeker est aveugle et immobile pendant 15 secondes..."));
    }

    private void equipSeeker(Player p) {
        p.getInventory().clear();
        ItemStack sword = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setUnbreakable(true);
        sword.setItemMeta(meta);
        p.getInventory().addItem(sword);

        p.getInventory().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
        p.getInventory().setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.GOLDEN_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.GOLDEN_BOOTS));

        if (hasScenario(Scenario.BOW)) {
            p.getInventory().addItem(new ItemStack(Material.BOW));
            p.getInventory().addItem(new ItemStack(Material.ARROW, 64));
        }
        if (hasScenario(Scenario.ANVIL)) {
            p.getInventory().addItem(taggedItem(Material.ANVIL, "anvil_power", "§7Clic droit : lâche une enclume sur les cachés"));
        }
    }

    private ItemStack taggedItem(Material mat, String tag, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_ITEM, PersistentDataType.STRING, tag);
        meta.lore(List.of(Msg.of(lore)));
        item.setItemMeta(meta);
        return item;
    }

    private void giveHiddenScenarioItems() {
        for (UUID id : aliveHidden) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            if (hasScenario(Scenario.TROLL_SWORD)) {
                p.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
            }
            if (hasScenario(Scenario.TNT_LAUNCHER)) {
                p.getInventory().addItem(taggedItem(Material.TNT, "tnt_launch", "§7Clic droit : propulse une TNT"));
            }
            if (hasScenario(Scenario.BLAZE_ROD)) {
                p.getInventory().addItem(taggedItem(Material.BLAZE_ROD, "blaze_lightning", "§7Clic droit : foudre sur le bloc visé"));
            }
            if (hasScenario(Scenario.HUNT_INFINITE)) {
                p.getInventory().addItem(taggedItem(Material.FIREWORK_ROCKET, "hunt_firework", "§7Clic droit : révèle la position d'un Seeker"));
            }
            // Montre de spectateur ajoutée uniquement à la mort
        }
    }

    private List<EntityType> buildWeightedMobPool() {
        map.rebalanceMobPercentages();
        List<EntityType> pool = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.getMobPercentages().entrySet()) {
            try {
                EntityType type = EntityType.valueOf(e.getKey().toUpperCase(Locale.ROOT));
                int weight = Math.max(1, e.getValue());
                for (int i = 0; i < weight; i++) pool.add(type);
            } catch (IllegalArgumentException ignored) {}
        }
        return pool;
    }

    private void teleportRandomInRegion(Player p) {
        Location loc = plugin.getGameManager().findRandomGroundLocation(map);
        if (loc != null) p.teleport(loc);
        else p.teleport(map.getLobby());
    }

    // --------------------------------------------------------- HUNT / MOBSWAP

    private void giveHuntFirework(int count) {
        for (UUID id : aliveHidden) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            ItemStack item = taggedItem(Material.FIREWORK_ROCKET, "hunt_firework", "§7Clic droit : révèle la position d'un Seeker");
            item.setAmount(Math.max(1, count));
            p.getInventory().addItem(item);
            p.sendMessage(Msg.of("§dVous avez reçu un feu d'artifice de traque !"));
        }
    }

    private void performMobSwap() {
        List<EntityType> pool = buildWeightedMobPool();
        for (UUID id : aliveHidden) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            plugin.getDisguiseManager().undisguise(p);
            EntityType type = pool.isEmpty() ? EntityType.PIG : pool.get(random.nextInt(pool.size()));
            disguises.put(id, type);
            plugin.getDisguiseManager().disguiseAsMob(p, type);
        }
        broadcastAll(Msg.of("§d§lLes apparences ont changé !"));
    }

    // -------------------------------------------------------------- DEATH

    public void onHiddenEliminated(Player victim) {
        aliveHidden.remove(victim.getUniqueId());
        plugin.getDisguiseManager().undisguise(victim);

        if (map.isVirusMode()) {
            seekers.add(victim.getUniqueId());
            victim.setGameMode(GameMode.SURVIVAL);
            equipSeeker(victim);
            victim.sendMessage(Msg.of("§c§lVous avez été infecté ! Vous êtes maintenant un Seeker."));
        } else {
            spectators.add(victim.getUniqueId());
            victim.setGameMode(GameMode.SPECTATOR);
            victim.getInventory().clear();
            ItemStack watch = taggedItem(Material.CLOCK, "leave_watch", "§7Clic pour rejoindre le hub");
            victim.getInventory().setItem(0, watch);
            victim.sendMessage(Msg.of("§7Vous avez été éliminé. Vous êtes maintenant spectateur."));
        }
        checkWinConditions();
    }

    public void registerSeekerKill(UUID seekerId) {
        killsUsed.merge(seekerId, 1, Integer::sum);
    }

    public boolean seekerHasKillsLeft(UUID seekerId) {
        if (hasScenario(Scenario.INFINITE_HITS)) return true;
        return getKillsUsed(seekerId) < map.getKillMax();
    }

    // ----------------------------------------------------------- COOLDOWNS

    public boolean tryUseAnvil(UUID uuid) {
        long now = System.currentTimeMillis();
        long ready = anvilCooldown.getOrDefault(uuid, 0L);
        if (now < ready) return false;
        anvilCooldown.put(uuid, now + 120_000L);
        return true;
    }

    public boolean tryUseBlazeRod(UUID uuid) {
        long now = System.currentTimeMillis();
        long ready = blazeCooldown.getOrDefault(uuid, 0L);
        if (now < ready) return false;
        blazeCooldown.put(uuid, now + 60_000L);
        return true;
    }

    // --------------------------------------------------------- WIN CHECK

    private void checkWinConditions() {
        if (state != GameState.RUNNING) return;
        if (aliveHidden.isEmpty()) {
            endGame(false, "§cLe Seeker a éliminé tout le monde !");
        }
    }

    public void endGame(boolean hiddenWin, String reasonMessage) {
        if (state == GameState.ENDING) return;
        state = GameState.ENDING;
        broadcastAll(Msg.of(reasonMessage));

        for (Player p : getAllOnlinePlayers()) {
            boolean won = hiddenWin ? !seekers.contains(p.getUniqueId()) : seekers.contains(p.getUniqueId());
            Title title = won
                    ? Title.title(Msg.of("§a§lVICTOIRE"), Msg.of("§7Bien joué !"),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500)))
                    : Title.title(Msg.of("§c§lDÉFAITE"), Msg.of("§7Ce sera pour la prochaine fois."),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500)));
            p.showTitle(title);
            p.setGameMode(GameMode.SPECTATOR);
            plugin.getDisguiseManager().undisguise(p);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getGameManager().resetSession(this), 200L);
    }

    private void broadcastAll(Component msg) {
        for (Player p : getAllOnlinePlayers()) p.sendMessage(msg);
    }

    public String getFormattedTimeLeft() {
        if (timeIndefinite || hasScenario(Scenario.INFINITE_TIME)) return "∞";
        int seconds = Math.max(0, ticksLeft / 20);
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    public EntityType getDisguise(UUID uuid) { return disguises.get(uuid); }
}
