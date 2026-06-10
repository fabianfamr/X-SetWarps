package com.fabian.xsetwarps.listeners;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.managers.LanguageManager;
import com.fabian.xsetwarps.utils.DebugLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {
    private final XSetWarps plugin;

    public PlayerJoinListener(XSetWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        DebugLogger.debug("PlayerJoinListener", "Player joined: " + player.getName());
        
        if (!player.hasPermission("xsetwarps.admin")) return;

        if (plugin.getConfig().getBoolean("check-updates", true)) {
            if (plugin.getUpdateChecker().isUpdateAvailable()) {
                DebugLogger.debug("PlayerJoinListener", "Notifying admin " + player.getName() + " about update");
                LanguageManager lang = plugin.getLanguageManager();
                String current = plugin.getDescription().getVersion();
                String latest = plugin.getUpdateChecker().getLatestVersion();
                
                player.sendMessage(lang.getMessage("update-available", "%current%", current, "%latest%", latest));
                player.sendMessage(lang.getMessage("update-download", "%url%", plugin.getUpdateChecker().getDownloadUrl()));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        DebugLogger.debug("PlayerJoinListener", "Player quit: " + event.getPlayer().getName());
        // Clean up cooldown data for disconnected players to prevent memory leaks
        plugin.getCooldownManager().clearCooldowns(event.getPlayer().getUniqueId());
        // Clean up GUI data
        plugin.getGuiManager().cleanupPlayer(event.getPlayer());
    }
}