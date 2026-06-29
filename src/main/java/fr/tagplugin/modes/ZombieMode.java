package fr.tagplugin.modes;
import fr.tagplugin.TagPlugin; import fr.tagplugin.game.GameManager; import fr.tagplugin.game.IGameMode; import fr.tagplugin.utils.MessageUtil;
import org.bukkit.entity.Player; import java.util.*;
public class ZombieMode implements IGameMode {
    private final TagPlugin plugin; private final GameManager manager;
    private final Set<Player> zombies = new HashSet<Player>(); private final List<Player> humans = new ArrayList<Player>();
    private Player firstZombie;
    public ZombieMode(TagPlugin p, GameManager m) { plugin=p; manager=m; }
    private String t(String k, String... r) { return plugin.getConfigManager().t(k, r); }
    public void start(List<Player> players) {
        List<Player> s = new ArrayList<Player>(players); Collections.shuffle(s);
        firstZombie = s.get(0); zombies.add(firstZombie);
        for (Player p : players) if (!p.equals(firstZombie)) humans.add(p);
        MessageUtil.send(firstZombie, t("zombie.zombie")); for (Player h : humans) MessageUtil.send(h, t("zombie.humain"));
        manager.broadcastToPlayers(t("zombie.annonce", "{zombie}", firstZombie.getName(), "{humains}", String.valueOf(humans.size())));
    }
    public void onTag(Player tagger, Player tagged) {
        if (!zombies.contains(tagger) || !humans.contains(tagged)) return;
        humans.remove(tagged); zombies.add(tagged);
        manager.notifyElimination(tagged, tagger);
        MessageUtil.send(tagged, t("zombie.infection")); manager.broadcastToPlayers(t("zombie.annonce-infection", "{zombie}", tagger.getName(), "{joueur}", tagged.getName(), "{restants}", String.valueOf(humans.size())));
        if (humans.isEmpty()) { manager.broadcastToPlayers(t("zombie.victoire-zombies")); manager.endGame(firstZombie); }
        else if (humans.size() == 1) { manager.broadcastToPlayers(t("zombie.dernier-humain", "{joueur}", humans.get(0).getName())); manager.endGame(humans.get(0)); }
    }
    public void onPlayerLeave(Player p) { humans.remove(p); zombies.remove(p); if (zombies.isEmpty() && !humans.isEmpty()) { firstZombie = humans.remove(0); zombies.add(firstZombie); } if (humans.isEmpty()) manager.endGame(firstZombie); }
    public Player getWinner() { return humans.isEmpty() ? firstZombie : humans.get(new Random().nextInt(humans.size())); }
    public void cleanup() { zombies.clear(); humans.clear(); }
}
