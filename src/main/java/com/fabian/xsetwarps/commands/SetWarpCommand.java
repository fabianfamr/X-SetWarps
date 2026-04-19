package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.model.Warp;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetWarpCommand implements CommandExecutor {
    private final XSetWarps plugin;

    public SetWarpCommand(XSetWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("only-players"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("xsetwarps.setwarp")) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "no-permission"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "usage-setwarp"));
            return true;
        }

        String warpName = args[0];

        // Build optional description from remaining args
        String description = "";
        if (args.length >= 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) sb.append(" ");
                sb.append(args[i]);
            }
            description = sb.toString();
        }

        Warp warp = new Warp(warpName, player.getLocation(), description, player.getName());
        boolean saved = plugin.getWarpManager().saveWarp(warp);

        if (!saved) {
            // max-warps limit reached
            int maxWarps = plugin.getConfig().getInt("warps.max-warps", -1);
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "warps-limit-reached", "%limit%", String.valueOf(maxWarps)));
            return true;
        }

        player.sendMessage(plugin.getLanguageManager().getMessage(player, "warp-set", "%warp%", warpName));
        return true;
    }
}
