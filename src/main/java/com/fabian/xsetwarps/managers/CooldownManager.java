package com.fabian.xsetwarps.managers;

import com.fabian.xsetwarps.XSetWarps;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    public CooldownManager(XSetWarps plugin) {
        // Plugin instance kept for future use if needed, or constructor simplified
    }

    // UUID -> (warpName -> timestamp of last use)
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    // UUID -> timestamp of last ANY warp use
    private final Map<UUID, Long> globalCooldowns = new HashMap<>();

    /**
     * Returns true if the player is still on cooldown for the given warp.
     */
    public boolean isOnCooldown(UUID uuid, String warpName, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return false;
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return false;
        Long lastUse = playerCooldowns.get(warpName.toLowerCase());
        if (lastUse == null) return false;
        return (System.currentTimeMillis() - lastUse) < (cooldownSeconds * 1000L);
    }

    /**
     * Returns the remaining cooldown in seconds (0 if not on cooldown).
     */
    public long getRemainingSeconds(UUID uuid, String warpName, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return 0;
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return 0;
        Long lastUse = playerCooldowns.get(warpName.toLowerCase());
        if (lastUse == null) return 0;
        long elapsed = System.currentTimeMillis() - lastUse;
        long remaining = (cooldownSeconds * 1000L) - elapsed;
        return remaining > 0 ? (remaining / 1000) + 1 : 0;
    }

    /**
     * Registers a warp use for the given player (sets the cooldown timestamp).
     */
    public void setCooldown(UUID uuid, String warpName) {
        cooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                 .put(warpName.toLowerCase(), System.currentTimeMillis());
    }

    /**
     * Returns the last warp name used by a player, or null if none.
     */
    public String getLastWarp(UUID uuid) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null || playerCooldowns.isEmpty()) return null;
        return playerCooldowns.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Returns true if the player is still on global cooldown for ANY warp.
     */
    public boolean isOnGlobalCooldown(UUID uuid, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return false;
        Long lastUse = globalCooldowns.get(uuid);
        if (lastUse == null) return false;
        return (System.currentTimeMillis() - lastUse) < (cooldownSeconds * 1000L);
    }

    /**
     * Returns the remaining global cooldown in seconds (0 if not on cooldown).
     */
    public long getGlobalRemainingSeconds(UUID uuid, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return 0;
        Long lastUse = globalCooldowns.get(uuid);
        if (lastUse == null) return 0;
        long elapsed = System.currentTimeMillis() - lastUse;
        long remaining = (cooldownSeconds * 1000L) - elapsed;
        return remaining > 0 ? (remaining / 1000) + 1 : 0;
    }

    /**
     * Registers a global warp use for the given player.
     */
    public void setGlobalCooldown(UUID uuid) {
        globalCooldowns.put(uuid, System.currentTimeMillis());
    }

    /**
     * Clears all cooldowns for a player (e.g. on disconnect).
     */
    public void clearCooldowns(UUID uuid) {
        cooldowns.remove(uuid);
        globalCooldowns.remove(uuid);
    }
}