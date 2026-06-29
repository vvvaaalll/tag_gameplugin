package fr.tagplugin.utils;
import org.bukkit.entity.Player;
import java.util.List;
public class MessageUtil {
    public static void send(Player p, String msg) { if (p != null && p.isOnline()) p.sendMessage(msg); }
    public static void broadcast(List<Player> players, String msg) { for (Player p : players) send(p, msg); }
}
