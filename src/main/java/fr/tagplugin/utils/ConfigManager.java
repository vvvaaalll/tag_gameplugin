package fr.tagplugin.utils;
import fr.tagplugin.TagPlugin;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
public class ConfigManager {
    private final TagPlugin plugin;
    private final Map<String, String> configValues = new HashMap<String, String>();
    private final Map<String, String> langValues   = new HashMap<String, String>();
    public String langue = "fr"; public int minPlayers = 3; public int voteDuration = 30;
    public int gameDuration = 180; public int roundDuration = 15;
    public int roiScoreToWin = 30; public double roiZoneRadius = 5.0;
    public String prefix = "[Tag] ";
    public ConfigManager(TagPlugin plugin) { this.plugin = plugin; loadConfig(); loadLang(); }
    public void reload() { configValues.clear(); langValues.clear(); loadConfig(); loadLang(); }
    private void loadConfig() {
        File f = new File(plugin.getDataFolder(), "config.yml");
        if (!f.exists()) { plugin.getDataFolder().mkdirs(); copyResource("config.yml", f); }
        parseYaml(f, configValues);
        langue = get(configValues, "langue", "fr"); minPlayers = getInt(configValues, "jeu.joueurs-minimum", 3);
        voteDuration = getInt(configValues, "jeu.duree-vote", 30); gameDuration = getInt(configValues, "jeu.duree-partie", 180);
        roundDuration = getInt(configValues, "jeu.duree-manche-patate", 15);
        roiScoreToWin = getInt(configValues, "jeu.roi-score-victoire", 30);
        roiZoneRadius = getDouble(configValues, "jeu.roi-rayon-zone", 5.0);
    }
    private void loadLang() {
        File f = new File(plugin.getDataFolder(), "lang/" + langue + ".yml");
        if (!f.exists()) { f.getParentFile().mkdirs(); copyResource("lang/" + langue + ".yml", f); }
        if (!f.exists()) { langue = "fr"; f = new File(plugin.getDataFolder(), "lang/fr.yml"); copyResource("lang/fr.yml", f); }
        parseYaml(f, langValues);
        prefix = get(langValues, "prefix", "[Tag] ");
    }
    public String t(String key, String... rep) {
        String val = langValues.get(key); if (val == null) return "[?" + key + "]";
        val = colorize(val);
        for (int i = 0; i + 1 < rep.length; i += 2) val = val.replace(rep[i], rep[i+1]);
        return val;
    }
    private void parseYaml(File file, Map<String, String> map) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line; String section = "";
            while ((line = r.readLine()) != null) {
                String trim = line.trim(); if (trim.startsWith("#") || trim.isEmpty() || !trim.contains(":")) continue;
                int indent = 0; for (char c : line.toCharArray()) { if (c == ' ') indent++; else break; }
                int ci = trim.indexOf(":"); String key = trim.substring(0, ci).trim(); String val = trim.substring(ci+1).trim();
                if (val.startsWith("\"") && val.endsWith("\"")) val = val.substring(1, val.length()-1);
                if (indent == 0) { section = key; if (!val.isEmpty()) map.put(key, val); }
                else { if (!val.isEmpty()) map.put(section + "." + key, val); }
            }
            r.close();
        } catch (Exception e) { TagPlugin.log.warning("[TagPlugin] Erreur lecture: " + file.getName()); }
    }
    private void copyResource(String path, File dest) {
        try {
            InputStream in = plugin.getClass().getClassLoader().getResourceAsStream(path);
            if (in == null) return; dest.getParentFile().mkdirs();
            OutputStream out = new FileOutputStream(dest); byte[] buf = new byte[1024]; int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close(); out.close();
        } catch (Exception e) { TagPlugin.log.warning("[TagPlugin] Erreur copie: " + e.getMessage()); }
    }
    private String get(Map<String, String> m, String k, String d) { String v = m.get(k); return v != null ? v : d; }
    private int getInt(Map<String, String> m, String k, int d) { try { return Integer.parseInt(get(m,k,String.valueOf(d))); } catch(Exception e){return d;} }
    private double getDouble(Map<String, String> m, String k, double d) { try { return Double.parseDouble(get(m,k,String.valueOf(d))); } catch(Exception e){return d;} }
    private String colorize(String s) { return s.replace("&", "§"); }
}
