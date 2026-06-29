package fr.tagplugin.commands;

import fr.tagplugin.TagPlugin;
import fr.tagplugin.game.GameManager;
import fr.tagplugin.game.GameMode;
import fr.tagplugin.game.GameState;
import fr.tagplugin.utils.MessageUtil;
import fr.tagplugin.utils.StatsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class TagCommand implements CommandExecutor {

    private final TagPlugin   plugin;
    private final GameManager manager;
    private final StatsManager stats;

    public TagCommand(TagPlugin plugin) {
        this.plugin  = plugin;
        this.manager = plugin.getGameManager();
        this.stats   = plugin.getStatsManager();
    }

    private String t(String key, String... rep) { return plugin.getConfigManager().t(key, rep); }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        String sub = args[0].toLowerCase();
        if      (sub.equals("join")       || sub.equals("j"))  cmdJoin(sender);
        else if (sub.equals("leave")      || sub.equals("l"))  cmdLeave(sender);
        else if (sub.equals("vote")       || sub.equals("v"))  cmdVote(sender, args);
        else if (sub.equals("stats")      || sub.equals("s"))  cmdStats(sender, args);
        else if (sub.equals("top"))                             cmdTop(sender, args);
        else if (sub.equals("setspawn"))                        cmdSetSpawn(sender);
        else if (sub.equals("stop"))                            cmdStop(sender);
        else if (sub.equals("status"))                          cmdStatus(sender);
        else if (sub.equals("reload"))                          cmdReload(sender);
        // --- v1.5 ---
        else if (sub.equals("forcemode") || sub.equals("fm"))  cmdForceMode(sender, args);
        else if (sub.equals("kick")      || sub.equals("k"))   cmdKick(sender, args);
        else if (sub.equals("resetstats")|| sub.equals("rs"))  cmdResetStats(sender, args);
        else sendHelp(sender);
        return true;
    }

    private void cmdJoin(CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player p = (Player) sender;
        if (manager.getState() == GameState.RUNNING) { MessageUtil.send(p, t("file.partie-en-cours")); return; }
        if (manager.isInGame(p) || manager.isInQueue(p)) { MessageUtil.send(p, t("file.deja-dans-file")); return; }
        if (manager.joinQueue(p)) MessageUtil.send(p, t("file.rejoint"));
    }

    private void cmdLeave(CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player p = (Player) sender;
        if (!manager.isInGame(p) && !manager.isInQueue(p)) { MessageUtil.send(p, t("file.pas-dans-jeu")); return; }
        manager.leaveQueue(p);
    }

    private void cmdVote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player p = (Player) sender;
        if (manager.getState() != GameState.VOTING) { MessageUtil.send(p, t("vote.pas-de-vote")); return; }
        if (!manager.isInGame(p)) { MessageUtil.send(p, t("file.pas-dans-jeu")); return; }
        if (args.length < 2) { MessageUtil.send(p, t("vote.usage")); return; }
        String c = args[1].toLowerCase();
        GameMode mode;
        if      (c.equals("glacon")   || c.equals("freeze"))    mode = GameMode.GLACON;
        else if (c.equals("loups")    || c.equals("wolves"))    mode = GameMode.LOUPS;
        else if (c.equals("patate")   || c.equals("hotpotato")) mode = GameMode.PATATE;
        else if (c.equals("chasseur") || c.equals("hunter"))    mode = GameMode.CHASSEUR;
        else if (c.equals("zombie"))                             mode = GameMode.ZOMBIE;
        else if (c.equals("bouclier") || c.equals("shield"))    mode = GameMode.BOUCLIER;
        else if (c.equals("roi")      || c.equals("hill"))      mode = GameMode.ROI;
        else { MessageUtil.send(p, t("vote.modes-dispo")); return; }
        manager.vote(p, mode);
    }

    private void cmdStats(CommandSender sender, String[] args) {
        String name = (args.length >= 2) ? args[1] : (sender instanceof Player ? sender.getName() : null);
        if (name == null) { sender.sendMessage(plugin.prefix + "§cUsage: /tag stats <joueur>"); return; }

        int wins   = stats.get(name, StatsManager.WINS);
        int played = stats.get(name, StatsManager.PLAYED);
        int losses = stats.get(name, StatsManager.LOSSES);
        int kills  = stats.get(name, StatsManager.KILLS);
        int elims  = stats.get(name, StatsManager.ELIMINATIONS);
        int streak = plugin.getRewardManager().getStreak(name);

        sender.sendMessage(t("stats.titre",    "{joueur}", name));
        sender.sendMessage(t("stats.parties",  "{val}", String.valueOf(played)));
        sender.sendMessage(t("stats.victoires","{val}", String.valueOf(wins)));
        sender.sendMessage(t("stats.defaites", "{val}", String.valueOf(losses)));
        sender.sendMessage(t("stats.kills",    "{val}", String.valueOf(kills)));
        sender.sendMessage(t("stats.elims",    "{val}", String.valueOf(elims)));
        if (streak > 0)
            sender.sendMessage(t("stats.streak", "{val}", String.valueOf(streak)));
        if (played > 0)
            sender.sendMessage(t("stats.ratio", "{val}", String.valueOf((wins * 100) / played) + "%"));
    }

    private void cmdTop(CommandSender sender, String[] args) {
        String stat = StatsManager.WINS; String label = "Victoires";
        if (args.length >= 2) {
            String arg = args[1].toLowerCase();
            if (arg.equals("kills"))  { stat = StatsManager.KILLS;  label = "Kills"; }
            else if (arg.equals("played")||arg.equals("parties")) { stat = StatsManager.PLAYED; label = "Parties"; }
            else if (arg.equals("elims")) { stat = StatsManager.ELIMINATIONS; label = "Eliminations"; }
        }
        List<Map.Entry<String, Integer>> top = stats.getTop(stat, 10);
        sender.sendMessage(t("stats.top-titre", "{label}", label));
        if (top.isEmpty()) { sender.sendMessage(t("stats.top-vide")); return; }
        int rank = 1;
        for (Map.Entry<String, Integer> e : top) {
            if (e.getValue() == 0 && rank > 1) break;
            sender.sendMessage(t("stats.top-ligne", "{rang}", String.valueOf(rank), "{joueur}", e.getKey(), "{val}", String.valueOf(e.getValue()), "{label}", label.toLowerCase()));
            rank++;
        }
    }

    private void cmdSetSpawn(CommandSender sender) {
        if (!(sender instanceof Player) || !sender.isOp()) { sender.sendMessage(t("spawn.op-requis")); return; }
        manager.setArenaSpawn(((Player) sender).getLocation());
        sender.sendMessage(t("spawn.defini"));
    }

    private void cmdStop(CommandSender sender) {
        if (!sender.isOp()) { sender.sendMessage(t("spawn.op-requis")); return; }
        manager.forceStop(); sender.sendMessage(t("jeu.arrete"));
    }

    private void cmdStatus(CommandSender sender) {
        sender.sendMessage(plugin.prefix + "§eEtat: §f" + manager.getState().name());
        sender.sendMessage(plugin.prefix + "§eFile: §f" + manager.getQueue().size());
        sender.sendMessage(plugin.prefix + "§eEn jeu: §f" + manager.getPlayers().size());
        if (manager.getCurrentMode() != null)
            sender.sendMessage(plugin.prefix + "§eMode: §f" + manager.getCurrentMode().getDisplayName());
        boolean hasSpawn = plugin.getSpawnManager().hasCustomSpawn();
        sender.sendMessage(plugin.prefix + "§eSpawn: §f" + (hasSpawn ? "§aDefini" : "§7Spawn du monde (defaut)"));
    }

    private void cmdReload(CommandSender sender) {
        if (!sender.isOp()) { sender.sendMessage(t("spawn.op-requis")); return; }
        plugin.getConfigManager().reload();
        plugin.minPlayers    = plugin.getConfigManager().minPlayers;
        plugin.voteDuration  = plugin.getConfigManager().voteDuration;
        plugin.gameDuration  = plugin.getConfigManager().gameDuration;
        plugin.roundDuration = plugin.getConfigManager().roundDuration;
        plugin.prefix        = plugin.getConfigManager().prefix;
        sender.sendMessage(plugin.prefix + "§aConfig rechargee!");
    }

    // ===================== v1.5 =====================

    private void cmdForceMode(CommandSender sender, String[] args) {
        if (!sender.isOp()) { sender.sendMessage(t("spawn.op-requis")); return; }
        if (args.length < 2) { sender.sendMessage(plugin.prefix + "§cUsage: /tag forcemode <mode>"); return; }
        if (manager.getState() != GameState.VOTING && manager.getState() != GameState.WAITING) {
            sender.sendMessage(plugin.prefix + "§cImpossible : une partie est deja en cours."); return;
        }
        String c = args[1].toLowerCase();
        GameMode mode;
        if      (c.equals("glacon")   || c.equals("freeze"))    mode = GameMode.GLACON;
        else if (c.equals("loups")    || c.equals("wolves"))    mode = GameMode.LOUPS;
        else if (c.equals("patate")   || c.equals("hotpotato")) mode = GameMode.PATATE;
        else if (c.equals("chasseur") || c.equals("hunter"))    mode = GameMode.CHASSEUR;
        else if (c.equals("zombie"))                             mode = GameMode.ZOMBIE;
        else if (c.equals("bouclier") || c.equals("shield"))    mode = GameMode.BOUCLIER;
        else if (c.equals("roi")      || c.equals("hill"))      mode = GameMode.ROI;
        else { sender.sendMessage(plugin.prefix + "§cMode inconnu. Modes: glacon, loups, patate, chasseur, zombie, bouclier, roi"); return; }
        manager.forceMode(mode);
        sender.sendMessage(plugin.prefix + "§aMode force: §f" + mode.getDisplayName());
    }

    private void cmdKick(CommandSender sender, String[] args) {
        if (!sender.isOp()) { sender.sendMessage(t("spawn.op-requis")); return; }
        if (args.length < 2) { sender.sendMessage(plugin.prefix + "§cUsage: /tag kick <joueur>"); return; }
        String targetName = args[1];
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) { sender.sendMessage(plugin.prefix + "§cJoueur introuvable ou hors ligne."); return; }
        if (!manager.isInGame(target) && !manager.isInQueue(target)) {
            sender.sendMessage(plugin.prefix + "§c" + target.getName() + " n'est pas dans la partie."); return;
        }
        manager.leaveQueue(target);
        target.sendMessage(plugin.prefix + "§cVous avez ete expulse de la partie par un admin.");
        sender.sendMessage(plugin.prefix + "§a" + target.getName() + " a ete expulse de la partie.");
    }

    private void cmdResetStats(CommandSender sender, String[] args) {
        if (!sender.isOp()) { sender.sendMessage(t("spawn.op-requis")); return; }
        if (args.length < 2) { sender.sendMessage(plugin.prefix + "§cUsage: /tag resetstats <joueur>"); return; }
        String name = args[1];
        stats.reset(name);
        plugin.getRewardManager().resetStreak(name);
        sender.sendMessage(plugin.prefix + "§aStats de §f" + name + "§a remises a zero.");
    }

    // ===================== AIDE =====================

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.prefix + "§e/tag join §7- Rejoindre");
        sender.sendMessage(plugin.prefix + "§e/tag leave §7- Quitter");
        sender.sendMessage(plugin.prefix + "§e/tag vote <mode> §7- Voter");
        sender.sendMessage(plugin.prefix + "§7  glacon, loups, patate, chasseur, zombie, bouclier, roi");
        sender.sendMessage(plugin.prefix + "§e/tag stats [joueur] §7- Statistiques");
        sender.sendMessage(plugin.prefix + "§e/tag top [wins|kills|played|elims] §7- Classement");
        sender.sendMessage(plugin.prefix + "§e/tag status §7- Statut");
        if (sender.isOp()) {
            sender.sendMessage(plugin.prefix + "§c/tag setspawn §7[OP] - Definir le spawn (sauvegarde)");
            sender.sendMessage(plugin.prefix + "§c/tag stop §7[OP]");
            sender.sendMessage(plugin.prefix + "§c/tag reload §7[OP]");
            sender.sendMessage(plugin.prefix + "§c/tag forcemode <mode> §7[OP] - Forcer un mode");
            sender.sendMessage(plugin.prefix + "§c/tag kick <joueur> §7[OP] - Expulser de la partie");
            sender.sendMessage(plugin.prefix + "§c/tag resetstats <joueur> §7[OP] - Remettre les stats a zero");
        }
    }
}
