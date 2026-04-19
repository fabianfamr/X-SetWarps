package com.fabian.xsetwarps;

import com.fabian.xsetwarps.commands.*;
import com.fabian.xsetwarps.listeners.PlayerJoinListener;
import com.fabian.xsetwarps.hooks.PlaceholderHook;
import com.fabian.xsetwarps.managers.CooldownManager;
import com.fabian.xsetwarps.managers.LanguageManager;
import com.fabian.xsetwarps.managers.WarpManager;
import com.fabian.xsetwarps.utils.UpdateChecker;
import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.configuration.file.FileConfiguration;

public class XSetWarps extends JavaPlugin {
    private WarpManager warpManager;
    private LanguageManager languageManager;
    private CooldownManager cooldownManager;
    private UpdateChecker updateChecker;

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

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // PlaceholderAPI soft hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
        }

        getServer().getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', "&8[&bX-SetWarps&8] &rhas been enabled successfully!"));
        
        if (getConfig().getBoolean("updates.check", true)) {
            updateChecker.checkForUpdates();
        }
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender()
                .sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&bX-SetWarps&8] &rhas been disabled."));
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


    private void updateConfig() {
        FileConfiguration config = getConfig();
        boolean changed = false;

        // Migration mapping: Old Key -> New Key
        String[][] migrations = {
            {"check-updates", "updates.check"},
            {"help-message", "commands.help-message"},
            {"max-warps", "warps.max-warps"},
            {"per-warp-permission", "warps.per-warp-permission"},
            {"cooldown", "teleport.cooldown"},
            {"delay", "teleport.delay"},
            {"delay-cancel-on-move", "teleport.cancel-on-move"},
            {"delay-move-tolerance", "teleport.move-tolerance"}
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
