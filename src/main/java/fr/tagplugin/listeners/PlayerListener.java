package fr.tagplugin.listeners;
import fr.tagplugin.TagPlugin; import fr.tagplugin.game.GameManager; import fr.tagplugin.game.GameMode; import fr.tagplugin.game.GameState;
import org.bukkit.Bukkit; import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler; import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent; import org.bukkit.event.player.PlayerInteractEntityEvent; import org.bukkit.event.player.PlayerQuitEvent;
import java.util.List;
public class PlayerListener implements Listener {
    private final TagPlugin plugin; private final GameManager manager;
    private static final double TAG_DISTANCE = 1.5;
    public PlayerListener(TagPlugin plugin) {
        this.plugin=plugin; this.manager=plugin.getGameManager();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable(){ public void run(){ checkProximityTags(); }}, 10L, 10L);
    }
    @EventHandler public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)||!(event.getEntity() instanceof Player)) return;
        if (manager.getState()!=GameState.RUNNING) return;
        Player tagger=(Player)event.getDamager(); Player tagged=(Player)event.getEntity();
        if (manager.getCurrentMode()==GameMode.ROI) return; // PvP reel en mode ROI
        if (!manager.isInGame(tagger)||!manager.isInGame(tagged)) return;
        event.setCancelled(true); handleTag(tagger, tagged);
    }
    @EventHandler public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)||manager.getState()!=GameState.RUNNING) return;
        if (manager.getCurrentMode()==GameMode.ROI) return;
        handleTag(event.getPlayer(), (Player)event.getRightClicked());
    }
    private void checkProximityTags() {
        if (manager.getState()!=GameState.RUNNING||manager.getCurrentMode()==GameMode.ROI) return;
        List<Player> players=manager.getPlayers();
        for (int i=0;i<players.size();i++) { Player p1=players.get(i); for (int j=i+1;j<players.size();j++) { Player p2=players.get(j); if (!p1.getWorld().equals(p2.getWorld())) continue; if (p1.getLocation().distance(p2.getLocation())<=TAG_DISTANCE&&manager.getActiveMode()!=null) { manager.getActiveMode().onTag(p1,p2); manager.getActiveMode().onTag(p2,p1); } } }
    }
    private void handleTag(Player tagger, Player tagged) {
        if (manager.getState()!=GameState.RUNNING||!manager.isInGame(tagger)||!manager.isInGame(tagged)||tagger.equals(tagged)) return;
        if (manager.getActiveMode()!=null) manager.getActiveMode().onTag(tagger, tagged);
    }
    @EventHandler public void onPlayerQuit(PlayerQuitEvent event) { Player p=event.getPlayer(); if (manager.isInGame(p)||manager.isInQueue(p)) manager.leaveQueue(p); }
}
