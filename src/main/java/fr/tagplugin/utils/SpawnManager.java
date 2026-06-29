package fr.tagplugin.utils;

import fr.tagplugin.TagPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Sauvegarde le spawn de l'arene dans plugins/TagPlugin/spawn.yml
 * Persiste entre les redemarrages jusqu'au prochain /tag setspawn
 */
public class SpawnManager {

    private final TagPlugin plugin;
    private final File spawnFile;
    private Location arenaSpawn = null;

    public SpawnManager(TagPlugin plugin) {
        this.plugin = plugin;
        this.spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
        load();
    }

    public Location getSpawn() {
        if (arenaSpawn != null) return arenaSpawn;
        // Fallback : spawn du monde par defaut
        World w = Bukkit.getWorlds().get(0);
        return w.getSpawnLocation();
    }

    public void setSpawn(Location loc) {
        this.arenaSpawn = loc.clone();
        save();
        TagPlugin.log.info("[TagPlugin] Spawn sauvegarde: " + loc.getWorld().getName()
                + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
    }

    public boolean hasCustomSpawn() {
        return arenaSpawn != null;
    }

    private void load() {
        if (!spawnFile.exists()) return;
        try {
            Map<String, String> values = new HashMap<String, String>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(spawnFile), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty() || !line.contains(":")) continue;
                int ci = line.indexOf(':');
                String key = line.substring(0, ci).trim();
                String val = line.substring(ci + 1).trim();
                values.put(key, val);
            }
            reader.close();

            String worldName = values.get("world");
            if (worldName == null) return;
            World world = Bukkit.getWorld(worldName);
            if (world == null) { TagPlugin.log.warning("[TagPlugin] Monde introuvable: " + worldName); return; }

            double x     = Double.parseDouble(values.getOrDefault("x", "0"));
            double y     = Double.parseDouble(values.getOrDefault("y", "64"));
            double z     = Double.parseDouble(values.getOrDefault("z", "0"));
            float  yaw   = Float.parseFloat(values.getOrDefault("yaw", "0"));
            float  pitch = Float.parseFloat(values.getOrDefault("pitch", "0"));

            arenaSpawn = new Location(world, x, y, z, yaw, pitch);
            TagPlugin.log.info("[TagPlugin] Spawn charge: " + worldName + " " + (int)x + " " + (int)y + " " + (int)z);
        } catch (Exception e) {
            TagPlugin.log.warning("[TagPlugin] Erreur lecture spawn.yml: " + e.getMessage());
        }
    }

    private void save() {
        if (arenaSpawn == null) return;
        try {
            plugin.getDataFolder().mkdirs();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(spawnFile), "UTF-8"));
            writer.write("# TagPlugin - Spawn de l'arene\n");
            writer.write("# Modifie via /tag setspawn en jeu\n");
            writer.write("world: " + arenaSpawn.getWorld().getName() + "\n");
            writer.write("x: " + arenaSpawn.getX() + "\n");
            writer.write("y: " + arenaSpawn.getY() + "\n");
            writer.write("z: " + arenaSpawn.getZ() + "\n");
            writer.write("yaw: " + arenaSpawn.getYaw() + "\n");
            writer.write("pitch: " + arenaSpawn.getPitch() + "\n");
            writer.close();
        } catch (Exception e) {
            TagPlugin.log.warning("[TagPlugin] Erreur sauvegarde spawn.yml: " + e.getMessage());
        }
    }
}
