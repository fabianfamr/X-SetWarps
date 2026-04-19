package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.managers.LanguageManager;
import com.fabian.xsetwarps.model.Warp;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
        LanguageManager lang = plugin.getLanguageManager();

        if (!sender.hasPermission("xsetwarps.warps")) {
            sender.sendMessage(lang.getMessage(sender, "no-permission"));
            return true;
        }

        if (plugin.getWarpManager().getWarpCount() == 0) {
            sender.sendMessage(lang.getMessage(sender, "warps-list-empty"));
            return true;
        }

        // Fallback: text list
        sender.sendMessage(lang.getMessage(sender, "warps-list-header",
                "%count%", String.valueOf(plugin.getWarpManager().getWarpCount())));
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
