package com.hideseek.cachecache.scoreboard;

import com.hideseek.cachecache.game.GameSession;
import com.hideseek.cachecache.map.Scenario;
import com.hideseek.cachecache.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.UUID;

/**
 * Un seul scoreboard (en réalité trois gabarits : caché / Seeker / spectateur) est créé
 * UNE FOIS par arène et réutilisé pour toutes les parties qui s'y dérouleront, au lieu
 * d'en recréer un nouveau par joueur à chaque tick. On ne met à jour que le texte des
 * lignes (via des Team dont on change juste le prefix), jamais les entrées elles-mêmes :
 * pas de flicker, pas de fuite de mémoire, une seule identité de scoreboard par arène.
 */
public class ScoreboardHandler {

    private static final int MAX_LINES = 10;

    private final GameSession session;

    private Scoreboard hiddenBoard;
    private Objective hiddenObjective;
    private Team[] hiddenLines;
    private Team hiddenNoCollide;

    private Scoreboard seekerBoard;
    private Objective seekerObjective;
    private Team[] seekerLines;
    private Team seekerNoCollide;

    private Scoreboard spectatorBoard;
    private Objective spectatorObjective;
    private Team[] spectatorLines;
    private Team spectatorNoCollide;

    public ScoreboardHandler(GameSession session) {
        this.session = session;
    }

    private void ensureBuilt() {
        if (hiddenBoard != null) return;
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return;

        hiddenBoard = sm.getNewScoreboard();
        hiddenObjective = hiddenBoard.registerNewObjective("cc_hidden", Criteria.DUMMY, Msg.of("§6§lCACHE-CACHE"));
        hiddenObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        hiddenLines = buildLines(hiddenBoard, hiddenObjective);
        hiddenNoCollide = registerNoCollisionTeam(hiddenBoard);

        seekerBoard = sm.getNewScoreboard();
        seekerObjective = seekerBoard.registerNewObjective("cc_seeker", Criteria.DUMMY, Msg.of("§6§lCACHE-CACHE"));
        seekerObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        seekerLines = buildLines(seekerBoard, seekerObjective);
        seekerNoCollide = registerNoCollisionTeam(seekerBoard);

        spectatorBoard = sm.getNewScoreboard();
        spectatorObjective = spectatorBoard.registerNewObjective("cc_spec", Criteria.DUMMY, Msg.of("§6§lCACHE-CACHE §7(spec)"));
        spectatorObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        spectatorLines = buildLines(spectatorBoard, spectatorObjective);
        spectatorNoCollide = registerNoCollisionTeam(spectatorBoard);
    }

    /**
     * Équipe dédiée avec la règle de collision "NEVER" : c'est le mécanisme officiel de
     * Minecraft pour désactiver la poussée d'un JOUEUR par d'autres entités. Depuis les
     * versions récentes (1.9+, renforcé encore en 1.21), la collision des joueurs est en
     * grande partie gérée via cette règle d'équipe plutôt que par le simple flag
     * Entity#setCollidable — d'où le fait que ça se comportait différemment en 1.8.
     */
    private Team registerNoCollisionTeam(Scoreboard board) {
        Team team = board.registerNewTeam("nocollide");
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        return team;
    }

    private Team[] buildLines(Scoreboard board, Objective objective) {
        Team[] lines = new Team[MAX_LINES];
        for (int i = 0; i < MAX_LINES; i++) {
            String entry = "§" + Integer.toHexString(i) + "§r"; // entrée invisible unique
            Team team = board.registerNewTeam("l" + i);
            team.addEntry(entry);
            objective.getScore(entry).setScore(MAX_LINES - i);
            lines[i] = team;
        }
        return lines;
    }

    /**
     * Assigne le bon scoreboard (caché/Seeker) au joueur. À appeler une seule fois quand
     * le joueur entre en partie (ou change de rôle), pas à chaque tick.
     */
    public void assign(Player p, boolean seeker) {
        ensureBuilt();
        if (hiddenBoard == null) return;
        p.setScoreboard(seeker ? seekerBoard : hiddenBoard);
        (seeker ? seekerNoCollide : hiddenNoCollide).addPlayer(p);
    }

    /** Scoreboard spectateur complet : temps, cachés restants, ET infos Seeker(s). */
    public void assignSpectator(Player p) {
        ensureBuilt();
        if (spectatorBoard == null) return;
        p.setScoreboard(spectatorBoard);
        spectatorNoCollide.addPlayer(p);
    }

    public void update() {
        ensureBuilt();
        if (hiddenBoard == null) return;

        String mapLine = "§7Map: §f" + session.getMap().getName();
        String sep = "§8—————————";
        String timeLabel = "§eTemps restant:";
        String timeValue = "§f" + session.getFormattedTimeLeft();
        String countLabel = "§aCachés restants:";
        String countValue = "§f" + session.getAlivePlayersCount();

        setLine(hiddenLines, 0, mapLine);
        setLine(hiddenLines, 1, sep);
        setLine(hiddenLines, 2, timeLabel);
        setLine(hiddenLines, 3, timeValue);
        setLine(hiddenLines, 4, sep);
        setLine(hiddenLines, 5, countLabel);
        setLine(hiddenLines, 6, countValue);
        for (int i = 7; i < MAX_LINES; i++) setLine(hiddenLines, i, "");

        setLine(seekerLines, 0, mapLine);
        setLine(seekerLines, 1, sep);
        setLine(seekerLines, 2, timeLabel);
        setLine(seekerLines, 3, timeValue);
        setLine(seekerLines, 4, sep);
        setLine(seekerLines, 5, countLabel);
        setLine(seekerLines, 6, countValue);
        setLine(seekerLines, 7, sep);
        setLine(seekerLines, 8, "§cVos coups:");

        boolean infinite = session.hasScenario(Scenario.INFINITE_HITS);
        int killMax = session.getMap().getKillMax();
        setLine(seekerLines, 9, infinite ? "§f∞" : "§f.../" + killMax);

        for (Player p : session.getAllOnlinePlayers()) {
            if (session.isSeeker(p.getUniqueId())) {
                int used = session.getKillsUsed(p.getUniqueId());
                String value = infinite ? "§f∞" : "§f" + used + "/" + killMax;
                seekerLines[9].prefix(Msg.of(value));
                // Note : comme le scoreboard "seeker" est partagé entre tous les Seekers de
                // l'arène, avec plusieurs Seekers actifs simultanément la ligne de coups
                // affiche celle du dernier Seeker mis à jour dans cette boucle.
            }
        }

        // Scoreboard spectateur : synthèse complète (temps, cachés, ET Seekers/coups).
        int seekerCount = session.getSeekers().size();
        int totalUsed = 0;
        for (UUID id : session.getSeekers()) totalUsed += session.getKillsUsed(id);
        int totalMax = infinite ? -1 : killMax * Math.max(1, seekerCount);

        setLine(spectatorLines, 0, mapLine);
        setLine(spectatorLines, 1, sep);
        setLine(spectatorLines, 2, timeLabel);
        setLine(spectatorLines, 3, timeValue);
        setLine(spectatorLines, 4, sep);
        setLine(spectatorLines, 5, countLabel);
        setLine(spectatorLines, 6, countValue);
        setLine(spectatorLines, 7, "§cSeekers: §f" + seekerCount);
        setLine(spectatorLines, 8, "§cCoups utilisés:");
        setLine(spectatorLines, 9, totalMax < 0 ? "§f∞" : "§f" + totalUsed + "/" + totalMax);
    }

    private void setLine(Team[] lines, int index, String text) {
        if (lines == null || index >= lines.length) return;
        lines[index].prefix(Msg.of(text.length() > 60 ? text.substring(0, 60) : text));
    }

    /** Réinitialise le joueur sur le scoreboard principal du serveur (fin de partie / sortie d'arène). */
    public static void reset(Player p) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm != null) p.setScoreboard(sm.getMainScoreboard());
    }
}
