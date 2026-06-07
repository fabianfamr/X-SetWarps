package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.model.Warp;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

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
        String identifier = Warp.DEFAULT_IDENTIFIER;
        StringBuilder descriptionBuilder = new StringBuilder();
        
        // Parse arguments: [name] [identifier] [description...]
        // If no identifier provided, use "warps" as default
        if (args.length >= 2) {
            // Check if second arg is an identifier (file exists) or part of description
            String potentialIdentifier = args[1].toLowerCase();
            File identifierFile = new File(plugin.getDataFolder(), "warps/" + potentialIdentifier + ".yml");
            
            if (identifierFile.exists() || args[1].equalsIgnoreCase("warps")) {
                // It's an identifier
                identifier = potentialIdentifier;
                // Remaining args are description
                for (int i = 2; i < args.length; i++) {
                    if (descriptionBuilder.length() > 0) descriptionBuilder.append(" ");
                    descriptionBuilder.append(args[i]);
                }
            } else {
                // It's part of description
                for (int i = 1; i < args.length; i++) {
                    if (descriptionBuilder.length() > 0) descriptionBuilder.append(" ");
                    descriptionBuilder.append(args[i]);
                }
            }
        }

        String description = descriptionBuilder.toString();
        Warp warp = new Warp(warpName, player.getLocation(), description, player.getName(), identifier);
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
