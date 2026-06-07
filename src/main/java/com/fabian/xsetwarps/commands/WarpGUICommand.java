package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.managers.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open the warp GUI
 * Usage: /warpgui or /warps
 */
public class WarpGUICommand implements CommandExecutor {
    private final XSetWarps plugin;

    public WarpGUICommand(XSetWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("only-players"));
            return true;
        }

        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("xsetwarps.warps") && !player.hasPermission("xsetwarps.warpgui")) {
            player.sendMessage(lang.getMessage(player, "no-permission"));
            return true;
        }

        // Check for direct warp teleport
        if (args.length > 0 && plugin.getWarpManager().warpExists(args[0])) {
            WarpCommand warpCommand = new WarpCommand(plugin);
            warpCommand.onCommand(sender, command, label, args);
            return true;
        }

        // Open GUI
        if (plugin.getConfig().getBoolean("gui.enabled", true)) {
            plugin.getGuiManager().openWarpsListGUI(player, 1);
        } else {
            player.performCommand("warps");
        }

        return true;
    }
}
