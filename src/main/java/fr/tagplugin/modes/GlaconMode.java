package fr.tagplugin.modes;
import fr.tagplugin.TagPlugin;
import fr.tagplugin.game.GameManager;
import fr.tagplugin.game.IGameMode;
import fr.tagplugin.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.*;
public class GlaconMode implements IGameMode {
    private final TagPlugin plugin; private final GameManager manager;
    private Player freezer; private final List<Player> runners = new ArrayList<Player>();
    private final Set<Player> frozen = new HashSet<Player>();
    private final Map<Player, Location> frozenLocations = new HashMap<Player, Location>();
    private int freezeTaskId = -1;
    public GlaconMode(TagPlugin p, GameManager m) { plugin = p; manager = m; }
    private String t(String k, String... r) { return plugin.getConfigManager().t(k, r); }
    public void start(List<Player> players) {
        freezer = players.get(new Random().nextInt(players.size()));
        for (Player p : players) if (!p.equals(freezer)) runners.add(p);
        MessageUtil.send(freezer, t("glacon.freezer"));
        for (Player r : runners) MessageUtil.send(r, t("glacon.runner"));
        manager.broadcastToPlayers(t("glacon.annonce", "{freezer}", freezer.getName()));
        freezeTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            public void run() {
                for (Player p : new ArrayList<Player>(frozen)) {
                    if (!p.isOnline()) { frozen.remove(p); frozenLocations.remove(p); continue; }
                    Location loc = frozenLocations.get(p);
                    if (loc == null) continue;
                    loc.setYaw(p.getLocation().getYaw()); loc.setPitch(p.getLocation().getPitch());
                    p.teleport(loc);
                }
            }
        }, 2L, 2L);
    }
    public void onTag(Player tagger, Player tagged) {
        if (tagger.equals(freezer)) {
            if (!runners.contains(tagged) || frozen.contains(tagged)) return;
            frozen.add(tagged); frozenLocations.put(tagged, tagged.getLocation().clone());
            manager.notifyElimination(tagged, freezer);
            MessageUtil.send(tagged, t("glacon.gele")); manager.broadcastToPlayers(t("glacon.annonce-gel", "{joueur}", tagged.getName()));
            if (frozen.size() >= runners.size()) { manager.broadcastToPlayers(t("glacon.victoire-freezer", "{freezer}", freezer.getName())); manager.endGame(freezer); }
        } else if (runners.contains(tagger) && frozen.contains(tagged)) {
            frozen.remove(tagged); frozenLocations.remove(tagged);
            MessageUtil.send(tagged, t("glacon.desgele")); manager.broadcastToPlayers(t("glacon.annonce-desgel", "{joueur}", tagged.getName(), "{sauveur}", tagger.getName()));
        }
    }
    public void onPlayerLeave(Player p) { runners.remove(p); frozen.remove(p); frozenLocations.remove(p); if (p.equals(freezer) && !runners.isEmpty()) { freezer = runners.remove(0); manager.broadcastToPlayers(t("glacon.nouveau-freezer", "{freezer}", freezer.getName())); } if (runners.isEmpty()) manager.endGame(freezer); }
    public Player getWinner() { return runners.isEmpty() ? freezer : (runners.size() > 0 ? runners.get(new Random().nextInt(runners.size())) : null); }
    public void cleanup() { if (freezeTaskId != -1) { Bukkit.getScheduler().cancelTask(freezeTaskId); freezeTaskId = -1; } runners.clear(); frozen.clear(); frozenLocations.clear(); }
}
