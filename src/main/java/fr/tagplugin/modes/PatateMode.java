package fr.tagplugin.modes;
import fr.tagplugin.TagPlugin; import fr.tagplugin.game.GameManager; import fr.tagplugin.game.IGameMode; import fr.tagplugin.utils.MessageUtil;
import org.bukkit.Bukkit; import org.bukkit.entity.Player; import java.util.*;
public class PatateMode implements IGameMode {
    private final TagPlugin plugin; private final GameManager manager;
    private final List<Player> alive = new ArrayList<Player>(); private Player holder;
    private int roundTaskId = -1; private int roundDuration; private int round = 0;
    public PatateMode(TagPlugin p, GameManager m) { plugin=p; manager=m; }
    private String t(String k, String... r) { return plugin.getConfigManager().t(k, r); }
    public void start(List<Player> players) {
        alive.addAll(players); roundDuration = plugin.roundDuration;
        holder = alive.get(new Random().nextInt(alive.size()));
        MessageUtil.send(holder, t("patate.holder")); for (Player p : alive) if (!p.equals(holder)) MessageUtil.send(p, t("patate.runner", "{holder}", holder.getName()));
        startRound();
    }
    private void startRound() {
        round++; if (round > 1) roundDuration = Math.max(5, roundDuration - 2);
        manager.broadcastToPlayers(t("patate.debut-manche", "{manche}", String.valueOf(round), "{temps}", String.valueOf(roundDuration), "{holder}", holder.getName()));
        final int duration = roundDuration;
        roundTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            int ticks = 0;
            public void run() {
                ticks++;
                if (ticks == duration / 2) manager.broadcastToPlayers(t("patate.alerte", "{holder}", holder.getName()));
                if (ticks >= duration) { Bukkit.getScheduler().cancelTask(roundTaskId); roundTaskId = -1; endRound(); }
            }
        }, 20L, 20L);
    }
    private void endRound() {
        manager.broadcastToPlayers(t("patate.elimine", "{joueur}", holder.getName()));
        manager.notifyElimination(holder, null);
        alive.remove(holder);
        if (alive.size() <= 1) { manager.broadcastToPlayers(t("patate.victoire", "{joueur}", alive.isEmpty() ? "?" : alive.get(0).getName())); manager.endGame(alive.isEmpty() ? null : alive.get(0)); return; }
        holder = alive.get(new Random().nextInt(alive.size())); startRound();
    }
    public void onTag(Player tagger, Player tagged) { if (!tagger.equals(holder) || !alive.contains(tagged)) return; holder = tagged; MessageUtil.send(tagged, t("patate.recu")); manager.broadcastToPlayers(t("patate.passe", "{joueur}", tagged.getName())); }
    public void onPlayerLeave(Player p) { if (p.equals(holder) && alive.size() > 1) { alive.remove(p); holder = alive.get(new Random().nextInt(alive.size())); } else { alive.remove(p); } if (alive.size() <= 1) manager.endGame(alive.isEmpty() ? null : alive.get(0)); }
    public Player getWinner() { return alive.isEmpty() ? null : alive.get(0); }
    public void cleanup() { if (roundTaskId != -1) { Bukkit.getScheduler().cancelTask(roundTaskId); roundTaskId = -1; } alive.clear(); }
}
