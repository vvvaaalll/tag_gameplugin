package fr.tagplugin.modes;
import fr.tagplugin.TagPlugin; import fr.tagplugin.game.GameManager; import fr.tagplugin.game.IGameMode; import fr.tagplugin.utils.MessageUtil;
import org.bukkit.entity.Player; import java.util.*;
public class LoupsMode implements IGameMode {
    private final TagPlugin plugin; private final GameManager manager;
    private final Set<Player> wolves = new HashSet<Player>(); private final List<Player> runners = new ArrayList<Player>();
    private Player firstWolf;
    public LoupsMode(TagPlugin p, GameManager m) { plugin=p; manager=m; }
    private String t(String k, String... r) { return plugin.getConfigManager().t(k, r); }
    public void start(List<Player> players) {
        List<Player> s = new ArrayList<Player>(players); Collections.shuffle(s);
        int wolfCount = players.size() >= 6 ? 2 : 1;
        for (int i = 0; i < wolfCount; i++) { wolves.add(s.get(i)); if (i == 0) firstWolf = s.get(i); }
        for (int i = wolfCount; i < s.size(); i++) runners.add(s.get(i));
        for (Player w : wolves) MessageUtil.send(w, t("loups.loup")); for (Player r : runners) MessageUtil.send(r, t("loups.runner"));
        manager.broadcastToPlayers(t("loups.annonce", "{nb}", String.valueOf(wolfCount)));
    }
    public void onTag(Player tagger, Player tagged) {
        if (!wolves.contains(tagger) || !runners.contains(tagged)) return;
        runners.remove(tagged); wolves.add(tagged);
        manager.notifyElimination(tagged, tagger);
        MessageUtil.send(tagged, t("loups.infecte")); manager.broadcastToPlayers(t("loups.annonce-infection", "{joueur}", tagged.getName(), "{restants}", String.valueOf(runners.size())));
        if (runners.isEmpty()) { manager.broadcastToPlayers(t("loups.victoire-loups")); manager.endGame(firstWolf); }
        else if (runners.size() == 1) manager.endGame(runners.get(0));
    }
    public void onPlayerLeave(Player p) { wolves.remove(p); runners.remove(p); if (wolves.isEmpty() && !runners.isEmpty()) { firstWolf = runners.remove(0); wolves.add(firstWolf); } if (runners.isEmpty()) manager.endGame(firstWolf); }
    public Player getWinner() { return runners.isEmpty() ? firstWolf : runners.get(new Random().nextInt(runners.size())); }
    public void cleanup() { wolves.clear(); runners.clear(); }
}
