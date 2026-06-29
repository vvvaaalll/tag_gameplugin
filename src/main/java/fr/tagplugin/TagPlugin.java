package fr.tagplugin;

import fr.tagplugin.commands.TagCommand;
import fr.tagplugin.game.GameManager;
import fr.tagplugin.listeners.PlayerListener;
import fr.tagplugin.utils.ConfigManager;
import fr.tagplugin.utils.RewardManager;
import fr.tagplugin.utils.SpawnManager;
import fr.tagplugin.utils.StatsManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class TagPlugin extends JavaPlugin {

    public static Logger log = Logger.getLogger("Minecraft");

    private ConfigManager configManager;
    private GameManager   gameManager;
    private StatsManager  statsManager;
    private SpawnManager  spawnManager;
    private RewardManager rewardManager;

    public int    minPlayers    = 3;
    public int    voteDuration  = 30;
    public int    gameDuration  = 180;
    public int    roundDuration = 15;
    public String prefix        = "[Tag] ";

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        statsManager  = new StatsManager(this);
        spawnManager  = new SpawnManager(this);
        rewardManager = new RewardManager(this, statsManager);
        gameManager   = new GameManager(this);

        minPlayers    = configManager.minPlayers;
        voteDuration  = configManager.voteDuration;
        gameDuration  = configManager.gameDuration;
        roundDuration = configManager.roundDuration;
        prefix        = configManager.prefix;

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getCommand("tag").setExecutor(new TagCommand(this));
        log.info("[TagPlugin] v1.5 active!");
    }

    @Override
    public void onDisable() {
        if (gameManager  != null) gameManager.forceStop();
        if (statsManager != null) statsManager.save();
        log.info("[TagPlugin] desactive.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public GameManager   getGameManager()   { return gameManager; }
    public StatsManager  getStatsManager()  { return statsManager; }
    public SpawnManager  getSpawnManager()  { return spawnManager; }
    public RewardManager getRewardManager() { return rewardManager; }
}
