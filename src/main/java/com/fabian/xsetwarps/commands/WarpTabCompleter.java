package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.models.Warp;
import com.fabian.xsetwarps.utils.DebugLogger;
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
        DebugLogger.debug("WarpTabCompleter", "Tab complete for command '" + command.getName() + "', args length: " + args.length);
        
        // Handle xsetwarp subcommands
        if (command.getName().equalsIgnoreCase("xsetwarp")) {
            if (args.length == 1) {
                List<String> subCommands = new ArrayList<>();
                boolean isAdmin = sender.hasPermission("xsetwarps.admin");
                boolean canExport = isAdmin || sender.hasPermission("xsetwarps.export");
                boolean canImport = isAdmin || sender.hasPermission("xsetwarps.import");

                if (isAdmin) {
                    subCommands.addAll(Arrays.asList("reload", "update", "version", "locate", "setwarpcooldown"));
                }
                if (canExport) {
                    subCommands.add("export");
                }
                if (canImport) {
                    subCommands.add("import");
                }
                StringUtil.copyPartialMatches(args[0], subCommands, completions);
                Collections.sort(completions);
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("locate")) {
                    // Suggest available languages
                    List<String> languages = plugin.getLanguageManager().getAvailableLanguages();
                    StringUtil.copyPartialMatches(args[1], languages, completions);
                    Collections.sort(completions);
                } else if (args[0].equalsIgnoreCase("import")) {
                    // Suggest: file names from exports/, plugin names, and "all"
                    completions.add("essentials");
                    completions.add("cmi");
                    completions.add("all");
                    // Suggest exported file names (without .yml)
                    java.io.File exportDir = new java.io.File(plugin.getDataFolder(), "exports");
                    if (exportDir.exists() && exportDir.isDirectory()) {
                        java.io.File[] files = exportDir.listFiles((dir, name) -> name.endsWith(".yml"));
                        if (files != null) {
                            for (java.io.File f : files) {
                                completions.add(f.getName().replace(".yml", ""));
                            }
                        }
                    }
                    StringUtil.copyPartialMatches(args[1], completions, completions);
                    Collections.sort(completions);
                } else if (args[0].equalsIgnoreCase("setwarpcooldown")) {
                    // Suggest warp names
                    for (Warp warp : plugin.getWarpManager().getAllWarps()) {
                        completions.add(warp.getName());
                    }
                    StringUtil.copyPartialMatches(args[1], completions, completions);
                    Collections.sort(completions);
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("setwarpcooldown")) {
                StringUtil.copyPartialMatches(args[2], Arrays.asList("default", "0", "5", "10", "30", "60"), completions);
                Collections.sort(completions);
            } else if (args.length == 3 && args[0].equalsIgnoreCase("export")) {
                // Suggest identifiers (file names from warps/)
                completions.addAll(plugin.getWarpManager().getIdentifiers());
                StringUtil.copyPartialMatches(args[2], completions, completions);
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
