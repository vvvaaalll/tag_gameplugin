package fr.tagplugin.modes;
import fr.tagplugin.TagPlugin; import fr.tagplugin.game.GameManager; import fr.tagplugin.game.IGameMode; import fr.tagplugin.utils.MessageUtil;
import org.bukkit.entity.Player; import java.util.*;
public class ChasseurMode implements IGameMode {
    private final TagPlugin plugin; private final GameManager manager;
    private Player hunter; private final List<Player> runners = new ArrayList<Player>(); private final List<Player> caught = new ArrayList<Player>();
    public ChasseurMode(TagPlugin p, GameManager m) { plugin=p; manager=m; }
    private String t(String k, String... r) { return plugin.getConfigManager().t(k, r); }
    public void start(List<Player> players) {
        hunter = players.get(new Random().nextInt(players.size()));
        for (Player p : players) if (!p.equals(hunter)) runners.add(p);
        MessageUtil.send(hunter, t("chasseur.chasseur")); for (Player r : runners) MessageUtil.send(r, t("chasseur.coureur"));
        manager.broadcastToPlayers(t("chasseur.annonce", "{chasseur}", hunter.getName(), "{coureurs}", String.valueOf(runners.size())));
    }
    public void onTag(Player tagger, Player tagged) {
        if (!tagger.equals(hunter) || !runners.contains(tagged) || caught.contains(tagged)) return;
        caught.add(tagged); runners.remove(tagged);
        manager.notifyElimination(tagged, hunter);
        MessageUtil.send(tagged, t("chasseur.capture")); manager.broadcastToPlayers(t("chasseur.annonce-capture", "{joueur}", tagged.getName(), "{restants}", String.valueOf(runners.size())));
        if (runners.isEmpty()) { manager.broadcastToPlayers(t("chasseur.victoire-chasseur", "{chasseur}", hunter.getName())); manager.endGame(hunter); }
    }
    public void onPlayerLeave(Player p) { runners.remove(p); caught.remove(p); if (p.equals(hunter) && !runners.isEmpty()) { hunter = runners.remove(0); manager.broadcastToPlayers(t("chasseur.nouveau-chasseur", "{chasseur}", hunter.getName())); } if (runners.isEmpty()) manager.endGame(hunter); }
    public Player getWinner() { return runners.isEmpty() ? hunter : runners.get(new Random().nextInt(runners.size())); }
    public void cleanup() { runners.clear(); caught.clear(); }
}
