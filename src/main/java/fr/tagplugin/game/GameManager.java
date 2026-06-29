package fr.tagplugin.game;

import fr.tagplugin.TagPlugin;
import fr.tagplugin.modes.*;
import fr.tagplugin.utils.MessageUtil;
import fr.tagplugin.utils.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class GameManager {

    private final TagPlugin plugin;
    private final List<Player> queue   = new ArrayList<Player>();
    private final List<Player> players = new ArrayList<Player>();
    private final Map<Player, GameMode> votes = new HashMap<Player, GameMode>();

    private GameState state       = GameState.WAITING;
    private GameMode  currentMode = null;
    private IGameMode activeMode  = null;

    private int voteTaskId = -1, gameTaskId = -1;
    private int voteCountdown, gameCountdown;

    public GameManager(TagPlugin plugin) { this.plugin = plugin; }

    private String t(String key, String... rep) { return plugin.getConfigManager().t(key, rep); }
    private StatsManager stats()  { return plugin.getStatsManager(); }

    // ===================== FILE =====================

    public boolean joinQueue(Player player) {
        if (queue.contains(player) || players.contains(player)) return false;
        if (state == GameState.RUNNING || state == GameState.ENDING) return false;
        queue.add(player);
        broadcastToAll(t("file.joueur-rejoint", "{joueur}", player.getName(),
                "{actuel}", String.valueOf(queue.size()), "{min}", String.valueOf(plugin.minPlayers)));
        if (queue.size() >= plugin.minPlayers && state == GameState.WAITING) startVote();
        return true;
    }

    public boolean leaveQueue(Player player) {
        if (!queue.contains(player) && !players.contains(player)) return false;
        queue.remove(player); players.remove(player); votes.remove(player);
        if (activeMode != null) activeMode.onPlayerLeave(player);
        MessageUtil.send(player, t("file.quitte"));
        if (players.size() < 2 && state == GameState.RUNNING) endGame(null);
        return true;
    }

    // ===================== VOTE =====================

    public void startVote() {
        state = GameState.VOTING; votes.clear();
        players.addAll(queue); queue.clear();
        for (Player p : players) {
            MessageUtil.send(p, t("vote.debut-titre"));
            MessageUtil.send(p, t("vote.option-glacon"));   MessageUtil.send(p, t("vote.option-loups"));
            MessageUtil.send(p, t("vote.option-patate"));   MessageUtil.send(p, t("vote.option-chasseur"));
            MessageUtil.send(p, t("vote.option-zombie"));   MessageUtil.send(p, t("vote.option-bouclier"));
            MessageUtil.send(p, t("vote.option-roi"));
            MessageUtil.send(p, t("vote.temps", "{temps}", String.valueOf(plugin.voteDuration)));
        }
        voteCountdown = plugin.voteDuration;
        voteTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            public void run() {
                voteCountdown--;
                if (voteCountdown <= 5 && voteCountdown > 0)
                    broadcastToPlayers(t("vote.fin-proche", "{temps}", String.valueOf(voteCountdown)));
                if (voteCountdown <= 0) { Bukkit.getScheduler().cancelTask(voteTaskId); voteTaskId = -1; resolveVote(); }
            }
        }, 20L, 20L);
    }

    public boolean vote(Player player, GameMode mode) {
        if (state != GameState.VOTING || !players.contains(player)) return false;
        votes.put(player, mode);
        MessageUtil.send(player, t("vote.enregistre", "{mode}", mode.getDisplayName()));
        return true;
    }

    private void resolveVote() {
        Map<GameMode, Integer> tally = new HashMap<GameMode, Integer>();
        for (GameMode gm : GameMode.values()) tally.put(gm, 0);
        for (GameMode gm : votes.values()) tally.put(gm, tally.get(gm) + 1);
        GameMode winner = null; int max = -1;
        for (Map.Entry<GameMode, Integer> e : tally.entrySet())
            if (e.getValue() > max) { max = e.getValue(); winner = e.getKey(); }
        if (winner == null || max == 0) { GameMode[] modes = GameMode.values(); winner = modes[new Random().nextInt(modes.length)]; }
        currentMode = winner;
        broadcastToPlayers(t("vote.mode-choisi", "{mode}", winner.getDisplayName()));
        startGame();
    }

    // ===================== JEU =====================

    public void startGame() {
        state = GameState.STARTING;
        Location spawn = getArenaSpawn();
        for (Player p : players) p.teleport(spawn);
        broadcastToPlayers(t("jeu.demarrage"));
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { public void run() { broadcastToPlayers("§c3..."); } }, 20L);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { public void run() { broadcastToPlayers("§c2..."); } }, 40L);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { public void run() { broadcastToPlayers("§c1..."); } }, 60L);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { public void run() { launchMode(); } }, 80L);
    }

    private void launchMode() {
        state = GameState.RUNNING;
        for (Player p : players) stats().increment(p.getName(), StatsManager.PLAYED);

        switch (currentMode) {
            case GLACON:   activeMode = new GlaconMode(plugin, this);    break;
            case LOUPS:    activeMode = new LoupsMode(plugin, this);     break;
            case PATATE:   activeMode = new PatateMode(plugin, this);    break;
            case CHASSEUR: activeMode = new ChasseurMode(plugin, this);  break;
            case ZOMBIE:   activeMode = new ZombieMode(plugin, this);    break;
            case BOUCLIER: activeMode = new BouclierMode(plugin, this);  break;
            case ROI:      activeMode = new RoiCollineMode(plugin, this);break;
        }
        activeMode.start(new ArrayList<Player>(players));

        gameCountdown = plugin.gameDuration;
        gameTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            public void run() {
                gameCountdown--;
                if (gameCountdown==60||gameCountdown==30||gameCountdown==10
                        ||gameCountdown==5||gameCountdown==3||gameCountdown==2||gameCountdown==1)
                    broadcastToPlayers(t("jeu.temps-restant", "{temps}", String.valueOf(gameCountdown)));
                if (gameCountdown <= 0) { Bukkit.getScheduler().cancelTask(gameTaskId); gameTaskId=-1; endGame(activeMode.getWinner()); }
            }
        }, 20L, 20L);
    }

    public void endGame(final Player winner) {
        if (state == GameState.ENDING || state == GameState.WAITING) return;
        state = GameState.ENDING;
        if (gameTaskId != -1) { Bukkit.getScheduler().cancelTask(gameTaskId); gameTaskId = -1; }
        if (voteTaskId != -1) { Bukkit.getScheduler().cancelTask(voteTaskId); voteTaskId = -1; }

        for (Player p : players) {
            if (winner != null && p.equals(winner)) {
                stats().increment(p.getName(), StatsManager.WINS);
                // Enregistrer victoire sur ce mode specifique
                plugin.getRewardManager().recordModeWin(p.getName(), currentMode);
                MessageUtil.send(p, t("stats.victoire-notif"));
            } else {
                stats().increment(p.getName(), StatsManager.LOSSES);
                plugin.getRewardManager().onLoss(p);
            }
        }

        // Declencher les recompenses APRES avoir incremente les stats
        if (winner != null) {
            plugin.getRewardManager().onGameEnd(winner, currentMode);
        }

        if (winner != null) broadcastToAll(t("jeu.gagnant", "{joueur}", winner.getName()));
        else broadcastToAll(t("jeu.termine"));

        if (activeMode != null) { activeMode.cleanup(); activeMode = null; }
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { public void run() { reset(); } }, 100L);
    }

    public void notifyElimination(Player eliminated, Player killer) {
        stats().increment(eliminated.getName(), StatsManager.ELIMINATIONS);
        if (killer != null) stats().increment(killer.getName(), StatsManager.KILLS);
    }

    private void reset() {
        players.clear(); votes.clear(); currentMode = null; state = GameState.WAITING;
        broadcastToAll(t("jeu.rejoindre"));
    }

    public void forceStop() {
        if (gameTaskId != -1) { Bukkit.getScheduler().cancelTask(gameTaskId); gameTaskId = -1; }
        if (voteTaskId != -1) { Bukkit.getScheduler().cancelTask(voteTaskId); voteTaskId = -1; }
        if (activeMode != null) { activeMode.cleanup(); activeMode = null; }
        players.clear(); queue.clear(); votes.clear(); state = GameState.WAITING;
    }

    // Utilise SpawnManager pour la persistance
    public Location getArenaSpawn()          { return plugin.getSpawnManager().getSpawn(); }
    public void setArenaSpawn(Location loc)  { plugin.getSpawnManager().setSpawn(loc); }

    public void broadcastToPlayers(String msg) { MessageUtil.broadcast(players, msg); }
    public void broadcastToAll(String msg) { for (Player p : Bukkit.getOnlinePlayers()) MessageUtil.send(p, msg); }
    public boolean isInGame(Player p)  { return players.contains(p); }
    public boolean isInQueue(Player p) { return queue.contains(p); }
    public List<Player> getPlayers()   { return players; }
    public List<Player> getQueue()     { return queue; }
    public GameState getState()        { return state; }

    public void forceMode(GameMode mode) {
        this.currentMode = mode;
    }
    
    public GameMode getCurrentMode()   { return currentMode; }
    public IGameMode getActiveMode()   { return activeMode; }
}
