package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.managers.LanguageManager;
import com.fabian.xsetwarps.models.Warp;
import com.fabian.xsetwarps.utils.DebugLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DelWarpCommand implements CommandExecutor {
    private final XSetWarps plugin;

    public DelWarpCommand(XSetWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        DebugLogger.debug("DelWarpCommand", "onCommand() called by " + sender.getName() + ", args: " + String.join(" ", args));
        LanguageManager lang = plugin.getLanguageManager();

        if (!sender.hasPermission("xsetwarps.delwarp")) {
            sender.sendMessage(lang.getMessage(sender, "no-permission"));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(lang.getMessage(sender, "usage-delwarp"));
            return true;
        }

        String warpName = args[0];
        Warp warp = plugin.getWarpManager().getWarp(warpName);

        if (warp == null) {
            sender.sendMessage(lang.getMessage(sender, "warp-not-found", "%warp%", warpName));
            return true;
        }

        plugin.getWarpManager().deleteWarp(warp.getName());
        DebugLogger.debug("DelWarpCommand", "Warp deleted: " + warp.getName());
        sender.sendMessage(lang.getMessage(sender, "warp-deleted", "%warp%", warp.getName()));
        return true;
    }
}
