package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.managers.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class MainCommand implements CommandExecutor {
    private final XSetWarps plugin;

    public MainCommand(XSetWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (!sender.hasPermission("xsetwarps.admin")) {
            sender.sendMessage(lang.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                plugin.getWarpManager().loadAllWarps();
                plugin.getLanguageManager().loadLanguage();
                plugin.getGuiManager().reload();
                sender.sendMessage(lang.getMessage("reload-success"));
                break;
            case "update":
                plugin.getUpdateChecker().checkForUpdates(sender);
                break;
            case "version":
                sender.sendMessage(lang.getMessage("version-info", "%version%", plugin.getDescription().getVersion()));
                break;
            case "locate":
                handleLocateCommand(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleLocateCommand(CommandSender sender, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (args.length < 2) {
            // Show current language and list available
            String current = plugin.getLanguageManager().getCurrentLanguage().toLowerCase();
            List<String> available = plugin.getLanguageManager().getAvailableLanguages();
            sender.sendMessage(lang.getMessage("language-changed", "%language%", current));
            sender.sendMessage(lang.getMessage("language-list", "%list%", String.join(", ", available)));
            return;
        }

        String newLang = args[1].toLowerCase();
        boolean success = plugin.getLanguageManager().setLanguage(newLang);

        if (success) {
            String current = plugin.getLanguageManager().getCurrentLanguage().toLowerCase();
            sender.sendMessage(lang.getMessage("language-changed", "%language%", current));
        } else {
            List<String> available = plugin.getLanguageManager().getAvailableLanguages();
            sender.sendMessage(lang.getMessage("language-not-found", "%list%", String.join(", ", available)));
        }
    }

    private void sendHelp(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();
        sender.sendMessage(lang.getMessage("help-header"));
        sender.sendMessage(lang.getMessage("help-reload"));
        sender.sendMessage(lang.getMessage("help-update"));
        sender.sendMessage(lang.getMessage("help-version"));
        sender.sendMessage(lang.getMessage("help-locate"));
        sender.sendMessage(lang.getMessage("help-setwarp"));
        sender.sendMessage(lang.getMessage("help-warp"));
        sender.sendMessage(lang.getMessage("help-delwarp"));
        sender.sendMessage(lang.getMessage("help-warps"));
        sender.sendMessage(lang.getMessage("help-warpinfo"));
    }
}
