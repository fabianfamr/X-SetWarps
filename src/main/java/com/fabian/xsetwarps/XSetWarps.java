package com.fabian.xsetwarps;

import com.fabian.xsetwarps.commands.*;
import com.fabian.xsetwarps.managers.GUIManager;
import com.fabian.xsetwarps.listeners.GUIListener;
import com.fabian.xsetwarps.listeners.PlayerJoinListener;
import com.fabian.xsetwarps.hooks.PlaceholderHook;
import com.fabian.xsetwarps.managers.DependencyManager;
import com.fabian.xsetwarps.managers.CooldownManager;
import com.fabian.xsetwarps.managers.LanguageManager;
import com.fabian.xsetwarps.managers.WarpManager;
import com.fabian.xsetwarps.metrics.Metrics;
import com.fabian.xsetwarps.utils.ColorUtils;
import com.fabian.xsetwarps.utils.CommandRegistrar;
import com.fabian.xsetwarps.utils.ConfigUpdater;
import com.fabian.xsetwarps.utils.DebugLogger;
import com.fabian.xsetwarps.utils.TeleportEffects;
import com.fabian.xsetwarps.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class XSetWarps extends JavaPlugin {

    private static XSetWarps instance;

    public static XSetWarps getInstance() {
        return instance;
    }

    public void logInfo(String message) {
        getLogger().info(message);
    }

    public void logWarning(String message) {
        getLogger().warning(message);
    }

    public void logError(String message) {
        getLogger().severe(message);
    }

    private WarpManager warpManager;
    private LanguageManager languageManager;
    private CooldownManager cooldownManager;
    private UpdateChecker updateChecker;
    private GUIManager guiManager;
    private TeleportEffects teleportEffects;

    @Override
    public void onLoad() {
        DebugLogger.debug("Lifecycle", "onLoad() called");
        logInfo("X-SetWarps Pre-Load Started...");
        new DependencyManager(this).loadDependencies();
    }

    @Override
    public void onEnable() {
        instance = this;

        try {
            // Initialize config managers first
            saveDefaultConfig();
            DebugLogger.debug("Config", "Default config saved/loaded");
            updateConfig();
            checkConfigCode();
            this.languageManager = new LanguageManager(this);
            DebugLogger.debug("Config", "LanguageManager initialized");
        } catch (Exception e) {
            DebugLogger.debug("Config", "Failed to initialize config managers", e);
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Load libraries (already loaded in onLoad, but DependencyManager is for runtime libs)
        // Dependencies are loaded in onLoad() via DependencyManager

        // Initialize remaining managers
        try {
            DebugLogger.debug("Init", "Initializing remaining managers...");
            this.warpManager = new WarpManager(this);
            DebugLogger.debug("Init", "WarpManager initialized");
            this.cooldownManager = new CooldownManager(this);
            this.guiManager = new GUIManager(this);
            this.teleportEffects = new TeleportEffects(this);
            DebugLogger.debug("Init", "All managers initialized");

            // Register Commands
            DebugLogger.debug("Command", "Registering commands...");
            CommandRegistrar registrar = new CommandRegistrar(this);
            WarpTabCompleter tabCompleter = new WarpTabCompleter(this);

            getCommand("xsetwarp").setExecutor(new MainCommand(this));
            getCommand("xsetwarp").setTabCompleter(tabCompleter);

            registrar.register("setwarp", new SetWarpCommand(this), null);
            registrar.register("warp", new WarpCommand(this), tabCompleter);
            registrar.register("delwarp", new DelWarpCommand(this), tabCompleter);
            registrar.register("warps", new WarpsCommand(this), null, "warpgui");
            registrar.register("warpinfo", new WarpInfoCommand(this), tabCompleter);
            DebugLogger.debug("Command", "All commands registered");

            // Register Listeners
            Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
            Bukkit.getPluginManager().registerEvents(new GUIListener(this, guiManager), this);
            DebugLogger.debug("Init", "Listeners registered");

            // PlaceholderAPI Integration
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                DebugLogger.debug("PAPI", "PlaceholderAPI found, registering hook");
                new PlaceholderHook(this).register();
            } else {
                DebugLogger.debug("PAPI", "PlaceholderAPI not found, skipping hook");
            }

        } catch (Exception e) {
            DebugLogger.debug("Init", "Failed to initialize managers", e);
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Check for updates
        if (getConfig().getBoolean("updates.check", true)) {
            DebugLogger.debug("Update", "Update checker enabled");
            this.updateChecker = new UpdateChecker(this);
            updateChecker.checkForUpdates();
        }

        // Initialize bStats Metrics
        setupMetrics();

        String version = getDescription().getVersion();
        String lang = getConfig().getString("language", "EN").toUpperCase();

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-SetWarps&8] &7----------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-SetWarps&8]   &aEnabled v" + version + "! Enjoy warping!"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-SetWarps&8]   &fStorage: &eYAML &7| &fLanguage: &e" + lang));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-SetWarps&8] &7----------------------------------------------"));
    }

    @Override
    public void onDisable() {
        DebugLogger.debug("Init", "Plugin disabling...");

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-SetWarps&8] &7----------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-SetWarps&8]   &cDisabled v" + getDescription().getVersion() + "! Out."));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-SetWarps&8] &7----------------------------------------------"));
    }

    private void setupMetrics() {
        if (getConfig().getBoolean("metrics", true)) {
            try {
                int pluginId = 31698;
                Metrics metrics = new Metrics(this, pluginId);
                metrics.addCustomChart(new Metrics.SimplePie("language", () ->
                        getConfig().getString("language", "en")));
                metrics.addCustomChart(new Metrics.SingleLineChart("warps_count", () ->
                        warpManager.getTotalWarpCount()));
                metrics.addCustomChart(new Metrics.SimplePie("gui_enabled", () ->
                        getConfig().getBoolean("gui.enabled", true) ? "true" : "false"));
                metrics.addCustomChart(new Metrics.SimplePie("per_warp_permission", () ->
                        getConfig().getBoolean("warps.per-warp-permission", false) ? "true" : "false"));
            } catch (Exception e) {
                logWarning("Could not start bStats Metrics: " + e.getMessage());
            }
        }
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public TeleportEffects getTeleportEffects() {
        return teleportEffects;
    }

    /**
     * Checks the config 'code' value. If the disk config code is older than
     * the JAR default code, the disk config is backed up and rebuilt from the
     * JAR resource so users always get the latest config structure.
     */
    private void checkConfigCode() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) return;

        // Read code from JAR default
        int jarCode = 0;
        try {
            YamlConfiguration jarDefaults = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(getResource("config.yml"), java.nio.charset.StandardCharsets.UTF_8));
            jarCode = jarDefaults.getInt("code", 0);
        } catch (Exception e) {
            logWarning("Could not read config code from JAR: " + e.getMessage());
            return;
        }

        // Read code from disk
        YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(configFile);
        int diskCode = diskConfig.getInt("code", 0);

        if (diskCode < jarCode) {
            DebugLogger.debug("Config", "Config code outdated (disk=" + diskCode + ", jar=" + jarCode + "), rebuilding...");
            logInfo("Config code outdated (disk=" + diskCode + ", jar=" + jarCode + "). Rebuilding config...");

            // Backup current config
            File backupFile = new File(getDataFolder(), "config_old.yml");
            try {
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logInfo("Old config backed up to config_old.yml");
            } catch (IOException e) {
                logWarning("Could not back up config: " + e.getMessage());
            }

            // Rebuild config: write JAR default to disk
            try {
                saveResource("config.yml", true);
                reloadConfig();
                logInfo("Config rebuilt successfully from JAR defaults (code=" + jarCode + ").");
            } catch (Exception e) {
                logError("Failed to rebuild config: " + e.getMessage());
            }
        }
    }

    private void updateConfig() {
        DebugLogger.debug("Config", "Running config migrations...");
        FileConfiguration config = getConfig();
        boolean changed = false;

        // Migration: old nested metrics.enabled → flat metrics
        if (config.contains("metrics.enabled")) {
            if (!config.contains("metrics")) {
                config.set("metrics", config.getBoolean("metrics.enabled", true));
            }
            config.set("metrics.enabled", null);
            if (config.isConfigurationSection("metrics")) {
                // If metrics section still has other keys, keep it; otherwise clean up
                if (config.getConfigurationSection("metrics").getKeys(false).isEmpty()) {
                    config.set("metrics", true);
                }
            }
            changed = true;
        }

        // Migration: old flat check-updates → nested updates.check
        if (config.contains("check-updates")) {
            if (!config.contains("updates.check")) {
                config.set("updates.check", config.getBoolean("check-updates", true));
            }
            config.set("check-updates", null);
            changed = true;
        }

        // Ensure updates.notify-on-join exists
        if (!config.contains("updates.notify-on-join")) {
            config.set("updates.notify-on-join", true);
            changed = true;
        }

        // Migration mapping: Old Flat Key -> New Hierarchical Key
        String[][] migrations = {
                { "help-message", "commands.help-message" },
                { "max-warps", "warps.max-warps" },
                { "per-warp-permission", "warps.per-warp-permission" },
                { "cooldown", "teleport.cooldown" },
                { "delay", "teleport.delay" },
                { "delay-cancel-on-move", "teleport.cancel-on-move" },
                { "delay-move-tolerance", "teleport.move-tolerance" }
        };

        for (String[] migration : migrations) {
            String oldKey = migration[0];
            String newKey = migration[1];
            if (config.contains(oldKey)) {
                // Only migrate if the new key doesn't exist yet
                if (!config.contains(newKey)) {
                    config.set(newKey, config.get(oldKey));
                }
                config.set(oldKey, null); // Remove old key
                changed = true;
            }
        }

        // Handle 'lenguage' typo migration specifically
        if (config.contains("lenguage")) {
            if (!config.contains("language")) {
                config.set("language", config.get("lenguage"));
            }
            config.set("lenguage", null);
            changed = true;
        }

        if (changed) {
            saveConfig();
            DebugLogger.debug("Config", "Config migrations applied, saved to disk");
            logInfo("Config.yml has been updated to the latest format.");
        } else {
            DebugLogger.debug("Config", "No config migrations needed");
        }

        // Run ConfigUpdater to add any missing keys from the JAR resource
        DebugLogger.debug("Config", "Running ConfigUpdater to add missing keys...");
        ConfigUpdater.update(this, "config.yml", new File(getDataFolder(), "config.yml"));
        reloadConfig();
        DebugLogger.debug("Config", "Config update complete");
    }
}