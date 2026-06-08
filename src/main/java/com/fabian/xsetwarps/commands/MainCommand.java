package com.fabian.xsetwarps.commands;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.managers.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

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
                plugin.reloadConfig();
                plugin.getWarpManager().loadAllWarps();
                plugin.getLanguageManager().loadLanguage();
                plugin.getGuiManager().reload();
                plugin.getTeleportEffects().loadSettings();
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
            case "export":
                handleExportCommand(sender, args);
                break;
            case "import":
                handleImportCommand(sender, args);
                break;
            case "import-essentials":
                handleImportEssentialsCommand(sender);
                break;
            case "import-cmi":
                handleImportCMICommand(sender);
                break;
            case "import-all":
                handleImportAllCommand(sender);
                break;
            case "setwarpcooldown":
                handleSetWarpCooldown(sender, args);
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
        sender.sendMessage(lang.getMessage("help-export"));
        sender.sendMessage(lang.getMessage("help-import"));
        sender.sendMessage(lang.getMessage("help-import-plugins"));
        sender.sendMessage(lang.getMessage("help-import-all"));
        sender.sendMessage(lang.getMessage("help-setwarpcooldown"));
        sender.sendMessage(lang.getMessage("help-setwarp"));
        sender.sendMessage(lang.getMessage("help-warp"));
        sender.sendMessage(lang.getMessage("help-delwarp"));
        sender.sendMessage(lang.getMessage("help-warps"));
        sender.sendMessage(lang.getMessage("help-warpinfo"));
    }

    private void handleExportCommand(CommandSender sender, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (args.length < 2) {
            sender.sendMessage(lang.getMessage(sender, "usage-export"));
            return;
        }

        String fileName = args[1];
        String identifier = args.length >= 3 ? args[2] : null;

        int count = plugin.getWarpManager().exportWarps(identifier, fileName);
        if (count >= 0) {
            sender.sendMessage(lang.getMessage(sender, "export-success", "%count%", String.valueOf(count), "%file%", fileName + ".yml"));
        } else {
            sender.sendMessage(lang.getMessage(sender, "export-failed"));
        }
    }

    private void handleImportCommand(CommandSender sender, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (args.length < 2) {
            sender.sendMessage(lang.getMessage(sender, "usage-import"));
            return;
        }

        String fileName = args[1];
        int count = plugin.getWarpManager().importWarps(fileName);
        if (count >= 0) {
            sender.sendMessage(lang.getMessage(sender, "import-success", "%count%", String.valueOf(count)));
            plugin.getWarpManager().loadAllWarps();
        } else if (count == -1) {
            sender.sendMessage(lang.getMessage(sender, "import-file-not-found", "%file%", fileName));
        }
    }

    private void handleImportEssentialsCommand(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();

        int count = plugin.getWarpManager().importEssentialsWarps();
        if (count >= 0) {
            sender.sendMessage(lang.getMessage(sender, "import-success", "%count%", String.valueOf(count)));
            plugin.getWarpManager().loadAllWarps();
        } else if (count == -2) {
            sender.sendMessage(lang.getMessage(sender, "import-essentials-not-found"));
        } else {
            sender.sendMessage(lang.getMessage(sender, "import-file-not-found", "%file%", "Essentials/warps.yml"));
        }
    }

    private void handleImportCMICommand(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();

        int count = plugin.getWarpManager().importCMIWarps();
        if (count >= 0) {
            sender.sendMessage(lang.getMessage(sender, "import-success", "%count%", String.valueOf(count)));
            plugin.getWarpManager().loadAllWarps();
        } else if (count == -2) {
            sender.sendMessage(lang.getMessage(sender, "import-cmi-not-found"));
        } else {
            sender.sendMessage(lang.getMessage(sender, "import-file-not-found", "%file%", "CMI/warps.yml"));
        }
    }

    private void handleImportAllCommand(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();

        Map<String, Integer> results = plugin.getWarpManager().importFromAllPlugins();
        if (results.isEmpty()) {
            sender.sendMessage(lang.getMessage(sender, "import-all-no-plugins"));
            return;
        }

        int total = 0;
        for (Map.Entry<String, Integer> entry : results.entrySet()) {
            int count = entry.getValue();
            if (count >= 0) {
                sender.sendMessage(lang.getMessage(sender, "import-all-result", "%plugin%", entry.getKey(), "%count%", String.valueOf(count)));
                total += count;
            } else if (count == -1) {
                sender.sendMessage(lang.getMessage(sender, "import-file-not-found", "%file%", entry.getKey() + "/warps.yml"));
            }
        }

        if (total > 0) {
            plugin.getWarpManager().loadAllWarps();
            sender.sendMessage(lang.getMessage(sender, "import-success", "%count%", String.valueOf(total)));
        }
    }

    private void handleSetWarpCooldown(CommandSender sender, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (args.length < 3) {
            sender.sendMessage(lang.getMessage(sender, "usage-setwarpcooldown"));
            return;
        }

        String warpName = args[1];
        String cooldownStr = args[2];
        int cooldown;

        if (cooldownStr.equalsIgnoreCase("default") || cooldownStr.equalsIgnoreCase("-1")) {
            cooldown = -1; // Use global default
        } else {
            try {
                cooldown = Integer.parseInt(cooldownStr);
                if (cooldown < -1) {
                    sender.sendMessage(lang.getMessage(sender, "setwarpcooldown-invalid"));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(lang.getMessage(sender, "setwarpcooldown-invalid"));
                return;
            }
        }

        com.fabian.xsetwarps.model.Warp warp = plugin.getWarpManager().getWarp(warpName);
        if (warp == null) {
            sender.sendMessage(lang.getMessage(sender, "warp-not-found", "%warp%", warpName));
            return;
        }

        warp.setCooldown(cooldown);
        // Re-save the warp to persist the change
        plugin.getWarpManager().saveWarp(warp);

        if (cooldown == -1) {
            sender.sendMessage(lang.getMessage(sender, "setwarpcooldown-reset", "%warp%", warp.getName()));
        } else {
            sender.sendMessage(lang.getMessage(sender, "setwarpcooldown-set", "%warp%", warp.getName(), "%seconds%", String.valueOf(cooldown)));
        }
    }
}
