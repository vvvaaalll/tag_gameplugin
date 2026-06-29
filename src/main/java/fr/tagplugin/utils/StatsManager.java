package fr.tagplugin.utils;

import fr.tagplugin.TagPlugin;

import java.io.*;
import java.util.*;

/**
 * Gestion des statistiques persistantes des joueurs.
 * Sauvegarde dans plugins/TagPlugin/stats.yml
 * Format : nomJoueur.stat=valeur
 */
public class StatsManager {

    private final TagPlugin plugin;
    private final File statsFile;

    // Cache en memoire : nom -> {stat -> valeur}
    private final Map<String, Map<String, Integer>> cache = new HashMap<String, Map<String, Integer>>();

    public static final String WINS         = "wins";
    public static final String PLAYED       = "played";
    public static final String LOSSES       = "losses";
    public static final String KILLS        = "kills";
    public static final String ELIMINATIONS = "eliminations";

    public StatsManager(TagPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    // ===================== LECTURE =====================

    public int get(String playerName, String stat) {
        Map<String, Integer> stats = cache.get(playerName.toLowerCase());
        if (stats == null) return 0;
        Integer val = stats.get(stat);
        return val != null ? val : 0;
    }

    public Map<String, Integer> getAll(String playerName) {
        Map<String, Integer> stats = cache.get(playerName.toLowerCase());
        if (stats == null) return new HashMap<String, Integer>();
        return new HashMap<String, Integer>(stats);
    }

    // ===================== ECRITURE =====================

    public void increment(String playerName, String stat) {
        increment(playerName, stat, 1);
    }

    public void increment(String playerName, String stat, int amount) {
        String key = playerName.toLowerCase();
        if (!cache.containsKey(key)) cache.put(key, new HashMap<String, Integer>());
        Map<String, Integer> stats = cache.get(key);
        int current = stats.containsKey(stat) ? stats.get(stat) : 0;
        stats.put(stat, current + amount);
        save();
    }

    // --- v1.5 ---
    public void reset(String playerName) {
        cache.remove(playerName.toLowerCase());
        save();
    }

    // ===================== CLASSEMENT =====================

    public List<Map.Entry<String, Integer>> getTop(String stat, int limit) {
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>();
        for (Map.Entry<String, Map<String, Integer>> entry : cache.entrySet()) {
            int val = entry.getValue().containsKey(stat) ? entry.getValue().get(stat) : 0;
            list.add(new AbstractMap.SimpleEntry<String, Integer>(entry.getKey(), val));
        }
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue() - a.getValue();
            }
        });
        return list.subList(0, Math.min(limit, list.size()));
    }

    // ===================== PERSISTANCE =====================

    private void load() {
        cache.clear();
        if (!statsFile.exists()) return;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(statsFile), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;
                int dot = line.indexOf('.');
                int eq  = line.indexOf('=');
                if (dot < 0 || eq < 0 || dot > eq) continue;
                String player = line.substring(0, dot).toLowerCase();
                String stat   = line.substring(dot + 1, eq).trim();
                int val;
                try { val = Integer.parseInt(line.substring(eq + 1).trim()); }
                catch (NumberFormatException e) { continue; }
                if (!cache.containsKey(player)) cache.put(player, new HashMap<String, Integer>());
                cache.get(player).put(stat, val);
            }
            reader.close();
            TagPlugin.log.info("[TagPlugin] Stats chargees pour " + cache.size() + " joueur(s).");
        } catch (Exception e) {
            TagPlugin.log.warning("[TagPlugin] Erreur lecture stats: " + e.getMessage());
        }
    }

    public void save() {
        try {
            plugin.getDataFolder().mkdirs();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(statsFile), "UTF-8"));
            writer.write("# TagPlugin - Statistiques joueurs\n");
            writer.write("# Format: joueur.stat=valeur\n");
            List<String> players = new ArrayList<String>(cache.keySet());
            Collections.sort(players);
            for (String player : players) {
                Map<String, Integer> stats = cache.get(player);
                for (Map.Entry<String, Integer> e : stats.entrySet()) {
                    writer.write(player + "." + e.getKey() + "=" + e.getValue() + "\n");
                }
            }
            writer.close();
        } catch (Exception e) {
            TagPlugin.log.warning("[TagPlugin] Erreur sauvegarde stats: " + e.getMessage());
        }
    }
}
