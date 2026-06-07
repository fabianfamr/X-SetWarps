package com.fabian.xsetwarps.utils;

import com.fabian.xsetwarps.XSetWarps;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
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
        loadSettings();
    }
    
    public void loadSettings() {
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
        
        try {
            Sound sound = Sound.valueOf(soundName);
            loc.getWorld().playSound(loc, sound, soundVolume, soundPitch);
        } catch (IllegalArgumentException e) {
            loc.getWorld().playSound(loc, Sound.valueOf("ENTITY_ENDERMAN_TELEPORT"), soundVolume, soundPitch);
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
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                playTeleportOut(dest);
            }
        }, 2L);
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public boolean isSoundEnabled() { return soundEnabled; }
}