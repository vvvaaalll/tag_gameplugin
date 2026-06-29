package fr.tagplugin.utils;

import fr.tagplugin.TagPlugin;
import fr.tagplugin.game.GameMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestion des recompenses et titres speciaux.
 * Verifie les declencheurs apres chaque fin de partie et
 * broadcast un message + affiche un titre au joueur concerne.
 */
public class RewardManager {

    private final TagPlugin plugin;
    private final StatsManager stats;

    // Streak en cours par joueur (remis a 0 a chaque defaite)
    private final Map<String, Integer> streaks = new HashMap<String, Integer>();

    // Jalons de streak de toute facon y a que moi qui lit ca 
    private static final int[] STREAK_MILESTONES = {2, 3, 5, 10};

    // Jalons de parties jouees
    private static final int[] PLAYED_MILESTONES = {10, 50, 100};

    public RewardManager(TagPlugin plugin, StatsManager stats) {
        this.plugin = plugin;
        this.stats = stats;
    }

    private String t(String key, String... rep) { return plugin.getConfigManager().t(key, rep); }

    /**
     * A appeler apres chaque fin de partie pour le gagnant et les perdants.
     */
    public void onGameEnd(Player winner, GameMode mode) {
        if (winner == null) return;

        String name = winner.getName();
        int wins   = stats.get(name, StatsManager.WINS);
        int played = stats.get(name, StatsManager.PLAYED);

        // Mise a jour du streak
        int streak = streaks.containsKey(name) ? streaks.get(name) + 1 : 1;
        streaks.put(name, streak);

        // 1. Premiere victoire
        if (wins == 1) {
            reward(winner,
                t("reward.first-win-title"),
                t("reward.first-win-subtitle"),
                t("reward.first-win-broadcast", "{joueur}", name));
            return; // Une seule recompense a la fois
        }

        // 2. Streak de victoires
        for (int milestone : STREAK_MILESTONES) {
            if (streak == milestone) {
                reward(winner,
                    t("reward.streak-title", "{streak}", String.valueOf(streak)),
                    t("reward.streak-subtitle", "{streak}", String.valueOf(streak)),
                    t("reward.streak-broadcast", "{joueur}", name, "{streak}", String.valueOf(streak)));
                return;
            }
        }

        // 3. Victoire sur un mode specifique (premiere fois)
        String modeKey = "win_mode_" + mode.name().toLowerCase();
        if (stats.get(name, modeKey) == 1) { // Vient d'etre incremente = premiere fois
            reward(winner,
                t("reward.mode-title", "{mode}", mode.getDisplayName()),
                t("reward.mode-subtitle", "{mode}", mode.getDisplayName()),
                t("reward.mode-broadcast", "{joueur}", name, "{mode}", mode.getDisplayName()));
            return;
        }

        // 4. Jalons de parties jouees
        for (int milestone : PLAYED_MILESTONES) {
            if (played == milestone) {
                reward(winner,
                    t("reward.played-title", "{nb}", String.valueOf(milestone)),
                    t("reward.played-subtitle", "{nb}", String.valueOf(milestone)),
                    t("reward.played-broadcast", "{joueur}", name, "{nb}", String.valueOf(milestone)));
                return;
            }
        }
    }

    /**
     * Appele quand un joueur perd — remet le streak a zero.
     */
    public void onLoss(Player player) {
        if (player != null) streaks.put(player.getName(), 0);
    }

    /**
     * Enregistre la victoire sur un mode specifique dans les stats.
     */
    public void recordModeWin(String playerName, GameMode mode) {
        String modeKey = "win_mode_" + mode.name().toLowerCase();
        stats.increment(playerName, modeKey);
    }

    /**
     * Affiche le titre au joueur et broadcast le message a tous.
     * CB1060 n'a pas de sendTitle() — on simule avec des messages encadres.
     */
    private void reward(Player player, String title, String subtitle, String broadcast) {
        // Titre simule via messages (CB1060 n'a pas Player.sendTitle)
        player.sendMessage("§6§l╔══════════════════╗");
        player.sendMessage("§6§l    " + title);
        player.sendMessage("§e    " + subtitle);
        player.sendMessage("§6§l╚══════════════════╝");

        // Broadcast a tous les joueurs en ligne
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.sendMessage(broadcast);
        }
    }

    public int getStreak(String playerName) {
        return streaks.containsKey(playerName) ? streaks.get(playerName) : 0;
    }
    public void resetStreak(String playerName) {
        streaks.remove(playerName);
    }
}
