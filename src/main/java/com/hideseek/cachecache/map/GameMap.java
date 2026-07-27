package com.hideseek.cachecache.map;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.*;

public class GameMap {

    public static final int LOBBY_PLATFORM_SIZE = 8;

    public static class HuntEntry {
        public int triggerTick;
        public int fireworkCount;
        public HuntEntry(int triggerTick, int fireworkCount) {
            this.triggerTick = triggerTick;
            this.fireworkCount = fireworkCount;
        }
    }

    private String name;

    private Location pos1;
    private Location pos2;
    private boolean posConfirmed = false;

    private Location spawnSeeker;
    private Location lobby;

    private int timeTicks = -1; // -1 = non défini
    private int killMax = -1;
    private int maxPlayers = -1;
    private int seekerCount = 1;
    private boolean virusMode = false;

    private final Map<String, Integer> mobPercentages = new LinkedHashMap<>();
    private final Map<String, Boolean> mobExplicit = new LinkedHashMap<>();
    private final List<HuntEntry> hunts = new ArrayList<>();
    private final Set<Scenario> scenarios = EnumSet.noneOf(Scenario.class);

    private boolean saved = false;
    private boolean inMaintenance = false; // en cours de /config
    private boolean running = false;

    public GameMap(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Location getPos1() { return pos1; }
    public void setPos1(Location pos1) { this.pos1 = pos1; this.posConfirmed = false; }

    public Location getPos2() { return pos2; }
    public void setPos2(Location pos2) { this.pos2 = pos2; this.posConfirmed = false; }

    public boolean isPosConfirmed() { return posConfirmed; }
    public void confirmPos() { this.posConfirmed = true; }

    public Location getSpawnSeeker() { return spawnSeeker; }
    public void setSpawnSeeker(Location spawnSeeker) { this.spawnSeeker = spawnSeeker; }

    public Location getLobby() { return lobby; }
    public void setLobby(Location lobby) { this.lobby = lobby; }

    public int getTimeTicks() { return timeTicks; }
    public void setTimeTicks(int timeTicks) { this.timeTicks = timeTicks; }

    public int getKillMax() { return killMax; }
    public void setKillMax(int killMax) { this.killMax = killMax; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public int getSeekerCount() { return seekerCount; }
    public void setSeekerCount(int seekerCount) { this.seekerCount = seekerCount; }

    public boolean isVirusMode() { return virusMode; }
    public void setVirusMode(boolean virusMode) { this.virusMode = virusMode; }

    public Map<String, Integer> getMobPercentages() { return mobPercentages; }
    public Map<String, Boolean> getMobExplicit() { return mobExplicit; }

    public List<HuntEntry> getHunts() { return hunts; }

    public Set<Scenario> getScenarios() { return scenarios; }

    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }

    public boolean isInMaintenance() { return inMaintenance; }
    public void setInMaintenance(boolean inMaintenance) { this.inMaintenance = inMaintenance; }

    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }

    /**
     * Renvoie la liste des paramètres manquants pour pouvoir sauvegarder la map, vide si tout est OK.
     */
    public List<String> getMissingRequirements() {
        List<String> missing = new ArrayList<>();
        if (pos1 == null) missing.add("pos1");
        if (pos2 == null) missing.add("pos2");
        if (pos1 != null && pos2 != null && !posConfirmed) missing.add("posconfirm");
        if (spawnSeeker == null) missing.add("spawnseek");
        if (lobby == null) missing.add("lobby");
        if (timeTicks <= 0) missing.add("time");
        if (killMax <= 0) missing.add("killmax");
        if (maxPlayers <= 0) missing.add("maxplayers");
        if (mobPercentages.isEmpty()) missing.add("au moins un mob (mob <mob> [%])");
        return missing;
    }

    /**
     * Normalise les pourcentages des mobs pour qu'ils totalisent 100, en répartissant
     * équitablement ceux qui n'ont pas de valeur explicite.
     */
    public void rebalanceMobPercentages() {
        int explicitTotal = 0;
        int autoCount = 0;
        for (Map.Entry<String, Integer> e : mobPercentages.entrySet()) {
            if (Boolean.TRUE.equals(mobExplicit.get(e.getKey()))) {
                explicitTotal += e.getValue();
            } else {
                autoCount++;
            }
        }
        int remaining = Math.max(0, 100 - explicitTotal);
        if (autoCount > 0) {
            int share = remaining / autoCount;
            int rest = remaining % autoCount;
            int i = 0;
            for (Map.Entry<String, Integer> e : mobPercentages.entrySet()) {
                if (!Boolean.TRUE.equals(mobExplicit.get(e.getKey()))) {
                    int value = share + (i < rest ? 1 : 0);
                    e.setValue(value);
                    i++;
                }
            }
        }
    }

    public List<EntityType> getPossibleMobTypes() {
        List<EntityType> types = new ArrayList<>();
        for (String s : mobPercentages.keySet()) {
            try {
                types.add(EntityType.valueOf(s.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }
        return types;
    }
}
