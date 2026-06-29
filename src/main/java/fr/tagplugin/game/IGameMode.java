package fr.tagplugin.game;
import org.bukkit.entity.Player;
import java.util.List;
public interface IGameMode {
    void start(List<Player> players);
    void onTag(Player tagger, Player tagged);
    void onPlayerLeave(Player player);
    Player getWinner();
    void cleanup();
}
