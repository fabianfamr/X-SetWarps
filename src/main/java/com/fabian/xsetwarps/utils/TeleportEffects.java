package com.fabian.xsetwarps.utils;

import com.fabian.xsetwarps.XSetWarps;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Handles teleport sound effects
 * Compatible with Minecraft 1.8.8+
 */
public class TeleportEffects {
    private final XSetWarps plugin;
    
    private boolean enabled = true;
    private boolean soundEnabled = true;
    
    // Sound to play on teleport
    private String soundName = "ENTITY_ENDERMAN_TELEPORT";
    private float soundVolume = 1.0f;
    private float soundPitch = 1.0f;
    
    public TeleportEffects(XSetWarps plugin) {
        this.plugin = plugin;
        DebugLogger.debug("TeleportEffects", "Initializing TeleportEffects...");
        loadSettings();
    }
    
    public void loadSettings() {
        DebugLogger.debug("TeleportEffects", "Reloading effect settings");
        FileConfiguration config = plugin.getConfig();
        
        enabled = config.getBoolean("teleport.effects.enabled", true);
        soundEnabled = config.getBoolean("teleport.effects.sound.enabled", true);
        
        if (soundEnabled) {
            soundName = config.getString("teleport.effects.sound.name", "ENTITY_ENDERMAN_TELEPORT");
            soundVolume = (float) config.getDouble("teleport.effects.sound.volume", 1.0);
            soundPitch = (float) config.getDouble("teleport.effects.sound.pitch", 1.0);
        }
    }
    
    /**
     * Play teleport sound at a location
     */
    public void playTeleportIn(Location loc) {
        if (!enabled || !soundEnabled || loc.getWorld() == null) return;
        DebugLogger.debug("TeleportEffects", "Playing teleport sound at " + loc.getWorld().getName() + " (" + soundName + ")");
        try {
            XSound sound = XSound.matchXSound(soundName).orElse(XSound.ENTITY_ENDERMAN_TELEPORT);
            org.bukkit.Sound parsed = sound.parseSound();
            if (parsed != null) {
                loc.getWorld().playSound(loc, parsed, soundVolume, soundPitch);
            }
        } catch (Exception e) {
            // Fallback: try direct Sound enum (1.13+)
            try {
                org.bukkit.Sound fallback = org.bukkit.Sound.valueOf("ENTITY_ENDERMAN_TELEPORT");
                loc.getWorld().playSound(loc, fallback, soundVolume, soundPitch);
            } catch (Exception ex) {
                DebugLogger.debug("TeleportEffects", "Failed to play teleport sound: " + soundName, ex);
                plugin.logWarning("Failed to play teleport sound: " + soundName);
            }
        }
    }
    
    /**
     * Play teleport sound at player's location
     */
    public void playTeleportIn(Player player) {
        if (player != null && player.isOnline()) {
            playTeleportIn(player.getLocation());
        }
    }
    
    /**
     * Play arrival sound at target location
     */
    public void playTeleportOut(Location loc) {
        playTeleportIn(loc); // Same sound for both departure and arrival
    }
    
    /**
     * Play full teleport effect (departure + arrival)
     */
    public void playFullTeleport(Player player) {
        if (player == null || !player.isOnline()) return;
        
        // Play departure sound
        playTeleportIn(player);
        
        // Schedule arrival sound
        final Location dest = player.getLocation().clone();
        SchedulerUtil.runAtEntityLater(plugin, player, () -> {
            if (player.isOnline()) {
                playTeleportOut(dest);
            }
        }, 2L);
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public boolean isSoundEnabled() { return soundEnabled; }
}