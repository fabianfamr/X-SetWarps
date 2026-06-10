package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.managers.LanguageManager;
import com.fabian.xsetwarps.model.Warp;
import com.fabian.xsetwarps.utils.DebugLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WarpsCommand implements CommandExecutor {
    private final XSetWarps plugin;

    public WarpsCommand(XSetWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        DebugLogger.debug("WarpsCommand", "onCommand() called by " + sender.getName());
        LanguageManager lang = plugin.getLanguageManager();

        if (!sender.hasPermission("xsetwarps.warps")) {
            sender.sendMessage(lang.getMessage(sender, "no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("only-players"));
            return true;
        }

        Player player = (Player) sender;
        
        // If GUI is enabled, open the warps list GUI
        if (plugin.getConfig().getBoolean("gui.enabled", true)) {
            DebugLogger.debug("WarpsCommand", "Opening GUI for " + sender.getName());
            plugin.getGuiManager().openWarpsListGUI(player, 1);
            return true;
        }

        // Fallback to text list if GUI disabled
        if (plugin.getWarpManager().getTotalWarpCount() == 0) {
            DebugLogger.debug("WarpsCommand", "No warps to list");
            sender.sendMessage(lang.getMessage(sender, "warps-list-empty"));
            return true;
        }

        sender.sendMessage(lang.getMessage(sender, "warps-list-header",
                "%count%", String.valueOf(plugin.getWarpManager().getTotalWarpCount())));
        DebugLogger.debug("WarpsCommand", "Listing " + plugin.getWarpManager().getTotalWarpCount() + " warp(s) via text");
        
        List<String> names = new ArrayList<>(plugin.getWarpManager().getWarpNames());
        Collections.sort(names);
        
        for (String name : names) {
            Warp warp = plugin.getWarpManager().getWarp(name);
            if (warp != null) {
                sender.sendMessage(lang.getMessage(sender, "warps-list-entry",
                        "%warp%", warp.getName(),
                        "%world%", warp.getWorldName(),
                        "%desc%", warp.getDescription().isEmpty() ? "-" : warp.getDescription()));
            }
        }
        return true;
    }
}
