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
import com.fabian.xsetwarps.utils.TeleportEffects;
import com.fabian.xsetwarps.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class XSetWarps extends JavaPlugin {
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
        // Load and update config
        saveDefaultConfig();
        updateConfig();

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

        if (getConfig().getBoolean("updates.check", true)) {
            updateChecker.checkForUpdates();
        }

        // bStats Metrics
        if (getConfig().getBoolean("metrics.enabled", true)) {
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

    private void updateConfig() {
        FileConfiguration config = getConfig();
        boolean changed = false;

        // Migration mapping: Old Key -> New Key
        String[][] migrations = {
                { "check-updates", "updates.check" },
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
            getLogger().info("Config.yml has been updated to the latest format (hierarchical).");
        }
    }
}
