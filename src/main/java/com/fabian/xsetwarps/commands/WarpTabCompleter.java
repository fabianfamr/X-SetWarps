package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.model.Warp;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WarpTabCompleter implements TabCompleter {
    private final XSetWarps plugin;

    public WarpTabCompleter(XSetWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Handle xsetwarp subcommands
        if (command.getName().equalsIgnoreCase("xsetwarp")) {
            if (!sender.hasPermission("xsetwarps.admin")) {
                return completions;
            }
            if (args.length == 1) {
                List<String> subCommands = Arrays.asList("reload", "update", "version", "locate");
                StringUtil.copyPartialMatches(args[0], subCommands, completions);
                Collections.sort(completions);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("locate")) {
                // Get available languages dynamically (lowercase)
                List<String> languages = plugin.getLanguageManager().getAvailableLanguages();
                StringUtil.copyPartialMatches(args[1], languages, completions);
                Collections.sort(completions);
            }
            return completions;
        }
        
        // Handle warp-related commands
        if (args.length == 1) {
            List<String> warpNames = new ArrayList<>();
            boolean emptyPublic = plugin.getConfig().getBoolean("warps.empty-permission-public", true);
            
            for (Warp warp : plugin.getWarpManager().getAllWarps()) {
                boolean hasAccess = true;
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (warp.getPermission() != null && !warp.getPermission().trim().isEmpty()) {
                        hasAccess = player.hasPermission(warp.getPermission());
                    } else if (!emptyPublic) {
                        hasAccess = player.hasPermission("xsetwarps.warp.*") || 
                                    player.hasPermission("xsetwarps.warp." + warp.getName().toLowerCase());
                    }
                }
                
                if (hasAccess) {
                    warpNames.add(warp.getName());
                }
            }
            
            StringUtil.copyPartialMatches(args[0], warpNames, completions);
            Collections.sort(completions);
        }
        
        return completions;
    }
}
