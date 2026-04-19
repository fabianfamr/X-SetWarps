package com.fabian.xsetwarps.hooks;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.model.Warp;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderHook extends PlaceholderExpansion {
    private final XSetWarps plugin;

    public PlaceholderHook(XSetWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "xsetwarps";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Fabian";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("total")) {
            return String.valueOf(plugin.getWarpManager().getWarpCount());
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

        return null;
    }
}
