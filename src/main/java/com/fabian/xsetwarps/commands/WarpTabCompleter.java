package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
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
        
        if (args.length == 1) {
            List<String> warpNames = new ArrayList<>(plugin.getWarpManager().getWarpNames());
            StringUtil.copyPartialMatches(args[0], warpNames, completions);
            Collections.sort(completions);
        }
        
        return completions;
    }
}
