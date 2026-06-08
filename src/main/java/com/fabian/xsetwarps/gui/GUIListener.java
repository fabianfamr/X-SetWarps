package com.fabian.xsetwarps.gui;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.events.WarpTeleportEvent;
import com.fabian.xsetwarps.managers.CooldownManager;
import com.fabian.xsetwarps.managers.LanguageManager;
import com.fabian.xsetwarps.model.Warp;
import com.fabian.xsetwarps.utils.SchedulerUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("deprecation")
public class GUIListener implements Listener {

    private final XSetWarps plugin;
    private final GUIManager guiManager;

    public GUIListener(XSetWarps plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof WarpGUIHolder)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        WarpGUIHolder holder = (WarpGUIHolder) event.getInventory().getHolder();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (guiManager.isCloseSlot(slot)) {
            SchedulerUtils.runTask(plugin, () -> player.closeInventory());
            return;
        }

        if (guiManager.isPrevPageSlot(slot) && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
            int prevPage = holder.getPage() - 1;
            if (prevPage >= 1) {
                guiManager.openWarpsListGUI(player, prevPage);
            }
            return;
        }

        if (guiManager.isNextPageSlot(slot) && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
            int nextPage = holder.getPage() + 1;
            guiManager.openWarpsListGUI(player, nextPage);
            return;
        }

        String warpName = guiManager.getWarpAtSlot(player, slot);
        if (warpName == null || warpName.isEmpty()) return;

        Warp warp = plugin.getWarpManager().getWarp(warpName);
        if (warp == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "warp-not-found", "%warp%", warpName));
            SchedulerUtils.runTask(plugin, () -> player.closeInventory());
            return;
        }

        LanguageManager lang = plugin.getLanguageManager();

        // Permission check
        // If warp has a custom permission → verify it
        // If permission is empty '' → depends on warps.empty-permission-public
        if (warp.getPermission() != null && !warp.getPermission().trim().isEmpty()) {
            if (!player.hasPermission(warp.getPermission())) {
                player.sendMessage(lang.getMessage(player, "no-permission"));
                return;
            }
        } else {
            boolean emptyPublic = plugin.getConfig().getBoolean("warps.empty-permission-public", true);
            if (!emptyPublic) {
                // Restricted: requires per-warp permission
                if (!player.hasPermission("xsetwarps.warp.*")
                        && !player.hasPermission("xsetwarps.warp." + warp.getName().toLowerCase())) {
                    player.sendMessage(lang.getMessage(player, "no-permission"));
                    return;
                }
            }
            // empty-permission-public: true → no extra check, anyone can use it
        }

        int cooldownSeconds = plugin.getConfig().getInt("teleport.cooldown", 0);
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (cooldownManager.isOnCooldown(player.getUniqueId(), warpName, cooldownSeconds)) {
            long remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), warpName, cooldownSeconds);
            player.sendMessage(lang.getMessage(player, "warp-cooldown",
                    "%warp%", warp.getName(),
                    "%seconds%", String.valueOf(remaining)));
            return;
        }

        SchedulerUtils.runTask(plugin, () -> player.closeInventory());

        int delaySeconds = plugin.getConfig().getInt("teleport.delay", 0);
        if (delaySeconds > 0) {
            performDelayedTeleport(player, warp, delaySeconds);
        } else {
            performTeleport(player, warp);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof WarpGUIHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof WarpGUIHolder)) return;

        Player player = (Player) event.getPlayer();
        guiManager.cleanupPlayer(player);
    }

    private void performDelayedTeleport(Player player, Warp warp, int delaySeconds) {
        LanguageManager lang = plugin.getLanguageManager();
        Location startLocation = player.getLocation().clone();
        double tolerance = plugin.getConfig().getDouble("teleport.move-tolerance", 0.5);
        player.sendMessage(lang.getMessage(player, "warp-delay",
                "%warp%", warp.getName(),
                "%seconds%", String.valueOf(delaySeconds)));

        SchedulerUtils.TaskWrapper[] taskWrapper = new SchedulerUtils.TaskWrapper[1];
        taskWrapper[0] = SchedulerUtils.runAtEntityTimer(plugin, player, new Runnable() {
            int ticksRemaining = delaySeconds * 20;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    if (taskWrapper[0] != null) taskWrapper[0].cancel();
                    return;
                }

                if (plugin.getConfig().getBoolean("teleport.cancel-on-move", true)) {
                    Location current = player.getLocation();
                    if (current.getWorld() != startLocation.getWorld()) {
                        player.sendMessage(lang.getMessage(player, "warp-delay-cancelled", "%warp%", warp.getName()));
                        if (taskWrapper[0] != null) taskWrapper[0].cancel();
                        return;
                    }
                    double dist = Math.sqrt(
                            Math.pow(current.getX() - startLocation.getX(), 2) +
                            Math.pow(current.getY() - startLocation.getY(), 2) +
                            Math.pow(current.getZ() - startLocation.getZ(), 2));
                    if (dist > tolerance) {
                        player.sendMessage(lang.getMessage(player, "warp-delay-cancelled", "%warp%", warp.getName()));
                        if (taskWrapper[0] != null) taskWrapper[0].cancel();
                        return;
                    }
                }

                ticksRemaining -= 1;
                if (ticksRemaining <= 0) {
                    performTeleport(player, warp);
                    if (taskWrapper[0] != null) taskWrapper[0].cancel();
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

        WarpTeleportEvent event = new WarpTeleportEvent(player, warp);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        int cooldownSeconds = plugin.getConfig().getInt("teleport.cooldown", 0);
        if (cooldownSeconds > 0) {
            plugin.getCooldownManager().setCooldown(player.getUniqueId(), warp.getName());
        }

        if (plugin.getConfig().getBoolean("teleport.effects.enabled", true)) {
            plugin.getTeleportEffects().playTeleportIn(player);
        }

        player.teleport(loc);

        if (plugin.getConfig().getBoolean("teleport.effects.enabled", true)) {
            plugin.getTeleportEffects().playTeleportOut(player.getLocation());
        }

        player.sendMessage(lang.getMessage(player, "warp-teleport", "%warp%", warp.getName()));
    }
}
