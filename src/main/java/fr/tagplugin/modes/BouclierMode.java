package fr.tagplugin.modes;
import fr.tagplugin.TagPlugin; import fr.tagplugin.game.GameManager; import fr.tagplugin.game.IGameMode; import fr.tagplugin.utils.MessageUtil;
import org.bukkit.Bukkit; import org.bukkit.entity.Player; import java.util.*;
public class BouclierMode implements IGameMode {
    private final TagPlugin plugin; private final GameManager manager;
    private Player hunter, shield, target; private final List<Player> others = new ArrayList<Player>();
    private int taskId = -1;
    public BouclierMode(TagPlugin p, GameManager m) { plugin=p; manager=m; }
    private String t(String k, String... r) { return plugin.getConfigManager().t(k, r); }
    public void start(List<Player> players) {
        List<Player> s = new ArrayList<Player>(players); Collections.shuffle(s);
        hunter=s.get(0); shield=s.get(1); target=s.get(2);
        for (int i=3;i<s.size();i++) others.add(s.get(i));
        MessageUtil.send(hunter,t("bouclier.chasseur")); MessageUtil.send(shield,t("bouclier.bouclier","{cible}",target.getName())); MessageUtil.send(target,t("bouclier.cible","{bouclier}",shield.getName()));
        manager.broadcastToPlayers(t("bouclier.annonce","{chasseur}",hunter.getName(),"{bouclier}",shield.getName(),"{cible}",target.getName()));
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() { public void run() { manager.broadcastToPlayers(t("bouclier.rappel","{chasseur}",hunter.getName(),"{bouclier}",shield.getName(),"{cible}",target.getName())); }}, 600L, 600L);
    }
    public void onTag(Player tagger, Player tagged) {
        if (!tagger.equals(hunter)) return;
        if (tagged.equals(shield)) { Player old=hunter; hunter=shield; shield=old; MessageUtil.send(hunter,t("bouclier.nouveau-chasseur")); MessageUtil.send(shield,t("bouclier.nouveau-bouclier","{cible}",target.getName())); manager.broadcastToPlayers(t("bouclier.echange","{ancien}",old.getName(),"{nouveau}",hunter.getName())); }
        else if (tagged.equals(target)) { manager.notifyElimination(target, hunter); manager.broadcastToPlayers(t("bouclier.victoire-chasseur","{chasseur}",hunter.getName(),"{cible}",target.getName())); manager.endGame(hunter); }
    }
    public void onPlayerLeave(Player p) { others.remove(p); if (p.equals(target)) manager.endGame(hunter); else if (p.equals(hunter)) { hunter = others.isEmpty() ? shield : others.remove(0); } else if (p.equals(shield)) { shield = others.isEmpty() ? hunter : others.remove(0); } }
    public Player getWinner() { return target; }
    public void cleanup() { if (taskId!=-1){Bukkit.getScheduler().cancelTask(taskId);taskId=-1;} others.clear(); }
}
