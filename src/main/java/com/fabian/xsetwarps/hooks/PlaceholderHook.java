package com.fabian.xsetwarps.hooks;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.model.Warp;
import com.fabian.xsetwarps.utils.DebugLogger;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PlaceholderHook extends PlaceholderExpansion {
    private final XSetWarps plugin;

    public PlaceholderHook(XSetWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "xsetwarps";
    }

    @Override
    public String getAuthor() {
        return "Fabian";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        DebugLogger.debug("PlaceholderHook", "onRequest() params: " + params);
        if (params.equalsIgnoreCase("total")) {
            return String.valueOf(plugin.getWarpManager().getTotalWarpCount());
        }

        if (player == null) return null;

        if (params.equalsIgnoreCase("last_warp")) {
            String last = plugin.getCooldownManager().getLastWarp(player.getUniqueId());
            return last != null ? last : "-";
        }

        if (params.startsWith("cooldown_")) {
            String warpName = params.substring(9);
            int cooldownSeconds = plugin.getConfig().getInt("teleport.cooldown", 0);
            long remaining = plugin.getCooldownManager().getRemainingSeconds(player.getUniqueId(), warpName, cooldownSeconds);
            DebugLogger.debug("PlaceholderHook", "Cooldown placeholder for warp \"" + warpName + "\": " + remaining + "s remaining");
            return String.valueOf(remaining);
        }

        if (params.startsWith("has_")) {
            String warpName = params.substring(4);
            return String.valueOf(player.getPlayer() != null && player.getPlayer().hasPermission("xsetwarps.warp." + warpName.toLowerCase()));
        }

        if (params.startsWith("warp_world_")) {
            String warpName = params.substring(11);
            Warp warp = plugin.getWarpManager().getWarp(warpName);
            return warp != null ? warp.getWorldName() : "-";
        }

        if (params.startsWith("warp_desc_")) {
            String warpName = params.substring(10);
            Warp warp = plugin.getWarpManager().getWarp(warpName);
            return warp != null ? warp.getDescription() : "-";
        }

        DebugLogger.debug("PlaceholderHook", "Unresolved placeholder: " + params);
        return null;
    }