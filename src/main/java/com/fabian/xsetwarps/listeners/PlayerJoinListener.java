package com.fabian.xsetwarps.listeners;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.managers.LanguageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final XSetWarps plugin;

    public PlayerJoinListener(XSetWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (!player.hasPermission("xsetwarps.admin")) return;

        if (plugin.getConfig().getBoolean("updates.notify-on-join", true)) {
            if (plugin.getUpdateChecker().isUpdateAvailable()) {
                LanguageManager lang = plugin.getLanguageManager();
                String current = plugin.getDescription().getVersion();
                String latest = plugin.getUpdateChecker().getLatestVersion();
                
                player.sendMessage(lang.getMessage("update-available", "%current%", current, "%latest%", latest));
                player.sendMessage(lang.getMessage("update-download", "%url%", plugin.getUpdateChecker().getDownloadUrl()));
            }
        }
    }
}
