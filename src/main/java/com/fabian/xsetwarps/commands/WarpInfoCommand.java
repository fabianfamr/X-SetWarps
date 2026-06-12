package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.managers.LanguageManager;
import com.fabian.xsetwarps.models.Warp;
import com.fabian.xsetwarps.utils.DebugLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WarpInfoCommand implements CommandExecutor {
    private final XSetWarps plugin;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public WarpInfoCommand(XSetWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        DebugLogger.debug("WarpInfoCommand", "onCommand() called by " + sender.getName() + ", args: " + String.join(" ", args));
        LanguageManager lang = plugin.getLanguageManager();

        if (!sender.hasPermission("xsetwarps.warpinfo")) {
            sender.sendMessage(lang.getMessage(sender, "no-permission"));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(lang.getMessage(sender, "usage-warpinfo"));
            return true;
        }

        String warpName = args[0];
        Warp warp = plugin.getWarpManager().getWarp(warpName);

        if (warp == null) {
            DebugLogger.debug("WarpInfoCommand", "Warp not found: " + warpName);
            sender.sendMessage(lang.getMessage(sender, "warp-not-found", "%warp%", warpName));
            return true;
        }

        String desc    = warp.getDescription().isEmpty() ? "-" : warp.getDescription();
        String coords  = String.format("%.1f, %.1f, %.1f", warp.getX(), warp.getY(), warp.getZ());
        String created = DATE_FORMAT.format(new Date(warp.getCreatedAt()));
        DebugLogger.debug("WarpInfoCommand", "Showing info for warp: " + warp.getName() + " at " + coords);

        sender.sendMessage(lang.getMessage(sender, "warpinfo-header",  "%warp%", warp.getName()));
        sender.sendMessage(lang.getMessage(sender, "warpinfo-desc",    "%desc%", desc));
        sender.sendMessage(lang.getMessage(sender, "warpinfo-world",   "%world%", warp.getWorldName()));
        sender.sendMessage(lang.getMessage(sender, "warpinfo-coords",  "%coords%", coords));
        sender.sendMessage(lang.getMessage(sender, "warpinfo-creator", "%creator%", warp.getCreatedBy()));
        sender.sendMessage(lang.getMessage(sender, "warpinfo-created", "%date%", created));
        return true;
    }
}
