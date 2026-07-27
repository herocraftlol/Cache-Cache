package com.hideseek.cachecache.scoreboard;

import com.hideseek.cachecache.util.Msg;

import com.hideseek.cachecache.game.GameSession;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardHandler {

    private final GameSession session;

    public ScoreboardHandler(GameSession session) {
        this.session = session;
    }

    public void update() {
        for (Player p : session.getAllOnlinePlayers()) {
            applyBoard(p);
        }
    }

    private void applyBoard(Player p) {
        ScoreboardManager sm = p.getServer().getScoreboardManager();
        if (sm == null) return;
        Scoreboard board = sm.getNewScoreboard();
        Objective obj = board.registerNewObjective("cc", Criteria.DUMMY,
                Msg.of("§6§lCACHE-CACHE"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 15;
        boolean isSeeker = session.isSeeker(p.getUniqueId());

        setLine(obj, line--, "§7Map: §f" + session.getMap().getName());
        setLine(obj, line--, "§8—————————");
        setLine(obj, line--, "§eTemps restant:");
        setLine(obj, line--, "§f" + session.getFormattedTimeLeft());
        setLine(obj, line--, "§8—————————");
        setLine(obj, line--, "§aCachés restants:");
        setLine(obj, line--, "§f" + session.getAlivePlayersCount());

        if (isSeeker) {
            setLine(obj, line--, "§8—————————");
            setLine(obj, line--, "§cVos coups:");
            int killMax = session.getMap().getKillMax();
            boolean infinite = session.hasScenario(com.hideseek.cachecache.map.Scenario.INFINITE_HITS);
            setLine(obj, line--, infinite ? "§f∞" : "§f" + session.getKillsUsed(p.getUniqueId()) + "/" + killMax);
        }

        p.setScoreboard(board);
    }

    private void setLine(Objective obj, int score, String text) {
        // Bukkit n'autorise pas deux entrées de scoreboard identiques : on pad avec des
        // codes couleurs invisibles (uniques par ligne) pour éviter toute collision.
        String unique = text + invisiblePad(score);
        obj.getScore(unique).setScore(score);
    }

    private String invisiblePad(int score) {
        StringBuilder sb = new StringBuilder("§r");
        for (int i = 0; i < score; i++) sb.append("§r");
        return sb.toString();
    }
}
