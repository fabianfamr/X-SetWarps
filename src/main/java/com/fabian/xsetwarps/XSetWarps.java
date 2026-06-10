package com.fabian.xsetwarps;

import com.fabian.xsetwarps.commands.*;
import com.fabian.xsetwarps.gui.GUIManager;
import com.fabian.xsetwarps.gui.GUIListener;
import com.fabian.xsetwarps.listeners.PlayerJoinListener;
import com.fabian.xsetwarps.hooks.PlaceholderHook;
import com.fabian.xsetwarps.managers.DependencyManager;
import com.fabian.xsetwarps.managers.CooldownManager;
import com.fabian.xsetwarps.managers.LanguageManager;
import com.fabian.xsetwarps.managers.WarpManager;
import com.fabian.xsetwarps.utils.ColorUtils;
import com.fabian.xsetwarps.utils.ConfigUpdater;
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
        getLogger().info("X-SetWarps Pre-Load Started...");
        new DependencyManager(this).loadDependencies();
    }

    @Override
    public void onEnable() {
        instance = this;

        // Load and update config
        saveDefaultConfig();
        updateConfig();
        checkConfigCode();

        // Initialize Managers
        this.languageManager = new LanguageManager(this);
        this.warpManager = new WarpManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.updateChecker = new UpdateChecker(this);
        this.guiManager = new GUIManager(this);
        this.teleportEffects = new TeleportEffects(this);

        // Register Commands
        getCommand("xsetwarp").setExecutor(new MainCommand(this));
        getCommand("setwarp").setExecutor(new SetWarpCommand(this));
        getCommand("warp").setExecutor(new WarpCommand(this));

        DelWarpCommand delWarpCommand = new DelWarpCommand(this);
        getCommand("delwarp").setExecutor(delWarpCommand);

        WarpsCommand warpsCommand = new WarpsCommand(this);
        getCommand("warps").setExecutor(warpsCommand);

        WarpInfoCommand warpInfoCommand = new WarpInfoCommand(this);
        getCommand("warpinfo").setExecutor(warpInfoCommand);

        // Register Tab Completers
        WarpTabCompleter tabCompleter = new WarpTabCompleter(this);
        getCommand("warp").setTabCompleter(tabCompleter);
        getCommand("delwarp").setTabCompleter(tabCompleter);
        getCommand("warpinfo").setTabCompleter(tabCompleter);
        getCommand("xsetwarp").setTabCompleter(tabCompleter);

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(this, guiManager), this);

        // PlaceholderAPI soft hook
        String papiStatus = "&cNot Found";
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            papiStatus = "&aRegistered";
        }

        // Stylized startup message
        String version = getDescription().getVersion();
        String lang = getConfig().getString("language", "EN").toUpperCase();

        getServer().getConsoleSender()
                .sendMessage(ColorUtils.translateColors("&8[&bX-SetWarps&8] &7Enabling X-SetWarps v" + version));
        getServer().getConsoleSender()
                .sendMessage(ColorUtils.translateColors("&8[&bX-SetWarps&8] &7Initializing storage backend: &fYAML"));
        getServer().getConsoleSender()
                .sendMessage(ColorUtils.translateColors("&8[&bX-SetWarps&8] &7Connecting to YAML database..."));
        getServer().getConsoleSender()
                .sendMessage(ColorUtils.translateColors("&8[&bX-SetWarps&8] &7YAML database connected and ready."));

        getServer().getConsoleSender().sendMessage(
                ColorUtils.translateColors("&8[&bX-SetWarps&8] &7----------------------------------------------"));
        getServer().getConsoleSender().sendMessage(
                ColorUtils.translateColors("&8[&bX-SetWarps&8]   &aEnabled v" + version + "! Enjoy warping!"));
        getServer().getConsoleSender().sendMessage(
                ColorUtils.translateColors("&8[&bX-SetWarps&8]   &fStorage: &eYAML &7| &fLanguage: &e" + lang));
        getServer().getConsoleSender()
                .sendMessage(ColorUtils.translateColors("&8[&bX-SetWarps&8]   &fPlaceholderAPI: " + papiStatus));
        getServer().getConsoleSender().sendMessage(
                ColorUtils.translateColors("&8[&bX-SetWarps&8] &7----------------------------------------------"));

        if (getConfig().getBoolean("check-updates", true)) {
            updateChecker.checkForUpdates();
        }

        // bStats Metrics
        if (getConfig().getBoolean("metrics", true)) {
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
        }
    }

    @Override
    public void onDisable() {
        String version = getDescription().getVersion();

        getServer().getConsoleSender().sendMessage(
                ColorUtils.translateColors("&8[&bX-SetWarps&8] &7----------------------------------------------"));
        getServer().getConsoleSender().sendMessage(
                ColorUtils.translateColors("&8[&bX-SetWarps&8]   &cDisabled v" + version + "! Goodbye."));
        getServer().getConsoleSender().sendMessage(
                ColorUtils.translateColors("&8[&bX-SetWarps&8] &7----------------------------------------------"));
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
            getLogger().warning("Could not read config code from JAR: " + e.getMessage());
            return;
        }

        // Read code from disk
        YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(configFile);
        int diskCode = diskConfig.getInt("code", 0);

        if (diskCode < jarCode) {
            getLogger().info("Config code outdated (disk=" + diskCode + ", jar=" + jarCode + "). Rebuilding config...");

            // Backup current config
            File backupFile = new File(getDataFolder(), "config_old.yml");
            try {
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                getLogger().info("Old config backed up to config_old.yml");
            } catch (IOException e) {
                getLogger().warning("Could not back up config: " + e.getMessage());
            }

            // Rebuild config: write JAR default to disk
            try {
                saveResource("config.yml", true);
                reloadConfig();
                getLogger().info("Config rebuilt successfully from JAR defaults (code=" + jarCode + ").");
            } catch (Exception e) {
                getLogger().severe("Failed to rebuild config: " + e.getMessage());
            }
        }
    }

    private void updateConfig() {
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

        // Migration: old nested updates.check → flat check-updates
        if (config.contains("updates.check")) {
            if (!config.contains("check-updates")) {
                config.set("check-updates", config.getBoolean("updates.check", true));
            }
            config.set("updates.check", null);
            changed = true;
        }

        // Migration: old nested updates.notify-on-join → (removed, now always notifies if check-updates is true)
        if (config.contains("updates.notify-on-join")) {
            config.set("updates.notify-on-join", null);
            changed = true;
        }

        // Clean up empty updates section
        if (config.isConfigurationSection("updates")) {
            if (config.getConfigurationSection("updates").getKeys(false).isEmpty()) {
                config.set("updates", null);
                changed = true;
            }
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
            getLogger().info("Config.yml has been updated to the latest format.");
        }

        // Run ConfigUpdater to add any missing keys from the JAR resource
        ConfigUpdater.update(this, "config.yml", new File(getDataFolder(), "config.yml"));
        reloadConfig();
    }
}