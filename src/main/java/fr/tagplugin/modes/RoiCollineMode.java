package fr.tagplugin.modes;
import fr.tagplugin.TagPlugin; import fr.tagplugin.game.GameManager; import fr.tagplugin.game.IGameMode; import fr.tagplugin.utils.MessageUtil;
import org.bukkit.Bukkit; import org.bukkit.Location; import org.bukkit.entity.Player; import java.util.*;
public class RoiCollineMode implements IGameMode {
    private final TagPlugin plugin; private final GameManager manager;
    private final List<Player> players = new ArrayList<Player>(); private final Map<Player,Integer> scores = new HashMap<Player,Integer>();
    private int checkTaskId=-1, announceTaskId=-1;
    private final int scoreToWin; private final double zoneRadius;
    public RoiCollineMode(TagPlugin p, GameManager m) { plugin=p; manager=m; scoreToWin=p.getConfigManager().roiScoreToWin; zoneRadius=p.getConfigManager().roiZoneRadius; }
    private String t(String k, String... r) { return plugin.getConfigManager().t(k, r); }
    public void start(List<Player> pl) {
        players.addAll(pl); for (Player p : players) scores.put(p, 0);
        manager.broadcastToPlayers(t("roi.debut","{rayon}",String.valueOf((int)zoneRadius),"{score}",String.valueOf(scoreToWin)));
        checkTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable(){ public void run(){ tickZone(); }}, 20L, 20L);
        announceTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable(){ public void run(){ announceScores(); }}, 300L, 300L);
    }
    private void tickZone() {
        Location center = manager.getArenaSpawn(); List<Player> inZone = new ArrayList<Player>();
        for (Player p : players) { if (!p.isOnline()||!p.getWorld().equals(center.getWorld())) continue; if (p.getLocation().distance(center)<=zoneRadius) inZone.add(p); }
        if (inZone.size()==1) {
            Player king=inZone.get(0); int ns=scores.get(king)+1; scores.put(king,ns);
            MessageUtil.send(king,t("roi.dans-zone","{score}",String.valueOf(ns),"{max}",String.valueOf(scoreToWin)));
            if (ns>=scoreToWin) { manager.broadcastToPlayers(t("roi.victoire","{joueur}",king.getName())); manager.endGame(king); return; }
            if (ns==scoreToWin/2) manager.broadcastToPlayers(t("roi.moitie","{joueur}",king.getName(),"{score}",String.valueOf(ns),"{max}",String.valueOf(scoreToWin)));
            else if (ns==(scoreToWin*3)/4) manager.broadcastToPlayers(t("roi.proche","{joueur}",king.getName(),"{score}",String.valueOf(ns),"{max}",String.valueOf(scoreToWin)));
        } else if (inZone.size()>1) { for (Player p : inZone) MessageUtil.send(p, t("roi.contestee")); }
    }
    private void announceScores() {
        List<Map.Entry<Player,Integer>> sorted = new ArrayList<Map.Entry<Player,Integer>>(scores.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<Player,Integer>>(){public int compare(Map.Entry<Player,Integer> a,Map.Entry<Player,Integer> b){return b.getValue()-a.getValue();}});
        manager.broadcastToPlayers(t("roi.classement-titre")); int rank=1;
        for (Map.Entry<Player,Integer> e : sorted) { manager.broadcastToPlayers(t("roi.classement-ligne","{rang}",String.valueOf(rank),"{joueur}",e.getKey().getName(),"{score}",String.valueOf(e.getValue()),"{max}",String.valueOf(scoreToWin))); rank++; if(rank>5)break; }
    }
    public void onTag(Player tagger, Player tagged) {}
    public void onPlayerLeave(Player p) { players.remove(p); scores.remove(p); if (players.size()<=1&&!players.isEmpty()) manager.endGame(players.get(0)); }
    public Player getWinner() { Player w=null; int max=-1; for (Map.Entry<Player,Integer> e:scores.entrySet()) if(e.getValue()>max){max=e.getValue();w=e.getKey();} return w; }
    public void cleanup() { if(checkTaskId!=-1){Bukkit.getScheduler().cancelTask(checkTaskId);checkTaskId=-1;} if(announceTaskId!=-1){Bukkit.getScheduler().cancelTask(announceTaskId);announceTaskId=-1;} players.clear(); scores.clear(); }
}
