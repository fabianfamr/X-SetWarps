package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.events.WarpTeleportEvent;
import com.fabian.xsetwarps.managers.CooldownManager;
import com.fabian.xsetwarps.managers.LanguageManager;
import com.fabian.xsetwarps.model.Warp;
import com.fabian.xsetwarps.utils.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarpCommand implements CommandExecutor {
    private final XSetWarps plugin;

    public WarpCommand(XSetWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("only-players"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("xsetwarps.warp")) {
            player.sendMessage(lang.getMessage(player, "no-permission"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(lang.getMessage(player, "usage-warp"));
            return true;
        }

        String warpName = args[0];
        Warp warp = plugin.getWarpManager().getWarp(warpName);

        if (warp == null) {
            player.sendMessage(lang.getMessage(player, "warp-not-found", "%warp%", warpName));
            return true;
        }

        // Permission check
        // If warp has a custom permission → verify it
        // If permission is empty '' → depends on warps.empty-permission-public
        if (warp.getPermission() != null && !warp.getPermission().trim().isEmpty()) {
            if (!player.hasPermission(warp.getPermission())) {
                player.sendMessage(lang.getMessage(player, "no-permission"));
                return true;
            }
        } else {
            boolean emptyPublic = plugin.getConfig().getBoolean("warps.empty-permission-public", true);
            if (!emptyPublic) {
                // Restricted: requires per-warp permission
                if (!player.hasPermission("xsetwarps.warp.*")
                        && !player.hasPermission("xsetwarps.warp." + warp.getName().toLowerCase())) {
                    player.sendMessage(lang.getMessage(player, "no-permission"));
                    return true;
                }
            }
            // empty-permission-public: true → no extra check, anyone can use it
        }

        // Per-warp cooldown check (respects warp.getEffectiveCooldown with global fallback)
        int globalCooldownSetting = plugin.getConfig().getInt("teleport.cooldown", 0);
        int cooldownSeconds = warp.getEffectiveCooldown(globalCooldownSetting);
        CooldownManager cooldownManager = plugin.getCooldownManager();

        // Bypass cooldown permission
        if (!player.hasPermission("xsetwarps.bypass.cooldown")) {
            if (cooldownManager.isOnCooldown(player.getUniqueId(), warpName, cooldownSeconds)) {
                long remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), warpName, cooldownSeconds);
                player.sendMessage(lang.getMessage(player, "warp-cooldown", "%warp%", warp.getName(), "%seconds%",
                        String.valueOf(remaining)));
                return true;
            }

            // Global cooldown check
            int globalCooldownSeconds = plugin.getConfig().getInt("teleport.global-cooldown", 0);
            if (cooldownManager.isOnGlobalCooldown(player.getUniqueId(), globalCooldownSeconds)) {
                long remaining = cooldownManager.getGlobalRemainingSeconds(player.getUniqueId(), globalCooldownSeconds);
                player.sendMessage(lang.getMessage(player, "warp-global-cooldown", "%seconds%",
                        String.valueOf(remaining)));
                return true;
            }
        }

        // Delay check
        int delaySeconds = plugin.getConfig().getInt("teleport.delay", 0);
        if (delaySeconds > 0) {
            performDelayedTeleport(player, warp, delaySeconds);
        } else {
            performTeleport(player, warp);
        }

        return true;
    }

    public void performDelayedTeleport(Player player, Warp warp, int delaySeconds) {
        LanguageManager lang = plugin.getLanguageManager();
        Location startLocation = player.getLocation().clone();
        double tolerance = plugin.getConfig().getDouble("teleport.move-tolerance", 0.5);
        player.sendMessage(lang.getMessage(player, "warp-delay", "%warp%", warp.getName(), "%seconds%",
                String.valueOf(delaySeconds)));

        SchedulerUtils.TaskWrapper taskWrapper[] = new SchedulerUtils.TaskWrapper[1];
        taskWrapper[0] = SchedulerUtils.runAtEntityTimer(plugin, player, new Runnable() {
            int ticksRemaining = delaySeconds * 20;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    if (taskWrapper[0] != null)
                        taskWrapper[0].cancel();
                    return;
                }

                // Check if player moved beyond tolerance
                if (plugin.getConfig().getBoolean("teleport.cancel-on-move", true)) {
                    Location current = player.getLocation();
                    if (current.getWorld() != startLocation.getWorld()) {
                        player.sendMessage(lang.getMessage(player, "warp-delay-cancelled", "%warp%", warp.getName()));
                        if (taskWrapper[0] != null)
                            taskWrapper[0].cancel();
                        return;
                    }
                    double dist = Math.sqrt(
                            Math.pow(current.getX() - startLocation.getX(), 2) +
                                    Math.pow(current.getY() - startLocation.getY(), 2) +
                                    Math.pow(current.getZ() - startLocation.getZ(), 2));
                    if (dist > tolerance) {
                        player.sendMessage(lang.getMessage(player, "warp-delay-cancelled", "%warp%", warp.getName()));
                        if (taskWrapper[0] != null)
                            taskWrapper[0].cancel();
                        return;
                    }
                }

                ticksRemaining -= 1;
                if (ticksRemaining <= 0) {
                    performTeleport(player, warp);
                    if (taskWrapper[0] != null)
                        taskWrapper[0].cancel();
                }
            }
        }, 1L, 1L);
    }

    private void performTeleport(Player player, Warp warp) {
        LanguageManager lang = plugin.getLanguageManager();

        Location loc = warp.getLocation();
        if (loc == null) {
            player.sendMessage(lang.getMessage(player, "world-not-loaded", "%world%", warp.getWorldName()));
            return;
        }

        // Fire WarpTeleportEvent
        WarpTeleportEvent event = new WarpTeleportEvent(player, warp);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        // Register per-warp cooldown BEFORE teleporting (respects warp-specific cooldown)
        int globalCooldownSetting = plugin.getConfig().getInt("teleport.cooldown", 0);
        int cooldownSeconds = warp.getEffectiveCooldown(globalCooldownSetting);
        if (cooldownSeconds > 0) {
            plugin.getCooldownManager().setCooldown(player.getUniqueId(), warp.getName());
        }

        // Register global cooldown
        int globalCooldownSeconds = plugin.getConfig().getInt("teleport.global-cooldown", 0);
        if (globalCooldownSeconds > 0) {
            plugin.getCooldownManager().setGlobalCooldown(player.getUniqueId());
        }

        // Play departure effects
        if (plugin.getConfig().getBoolean("teleport.effects.enabled", true)) {
            plugin.getTeleportEffects().playTeleportIn(player);
        }

        player.teleport(loc);

        // Play arrival effects
        if (plugin.getConfig().getBoolean("teleport.effects.enabled", true)) {
            plugin.getTeleportEffects().playTeleportOut(player.getLocation());
        }

        player.sendMessage(lang.getMessage(player, "warp-teleport", "%warp%", warp.getName()));
    }
}
