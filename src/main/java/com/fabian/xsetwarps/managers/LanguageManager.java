package com.fabian.xsetwarps.managers;

import com.fabian.xsetwarps.XSetWarps;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LanguageManager {
    private final XSetWarps plugin;
    private FileConfiguration langConfig;
    private String langName;

    public LanguageManager(XSetWarps plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        // Fix typo 'lenguage' if it exists, otherwise use 'language'
        this.langName = plugin.getConfig().contains("language") ? 
                plugin.getConfig().getString("language") : 
                plugin.getConfig().getString("lenguage", "EN");

        // Ensure the folder exists
        File langFolder = new File(plugin.getDataFolder(), "languages");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // List of default languages to export from JAR if they don't exist
        String[] defaultLangs = { "EN", "ES", "PT", "JA", "RU", "CUSTOM" };
        for (String lang : defaultLangs) {
            File file = new File(langFolder, lang + ".yml");
            if (!file.exists()) {
                try {
                    plugin.saveResource("languages/" + lang + ".yml", false);
                } catch (Exception ignored) {
                    // Resource doesn't exist in JAR, ignore
                }
            }
        }

        File langFile = new File(langFolder, langName + ".yml");

        // If the user set a custom language that isn't in the JAR and doesn't exist on disk
        if (!langFile.exists()) {
            try {
                plugin.saveResource("languages/" + langName + ".yml", false);
            } catch (Exception e) {
                // Not in JAR, using EN as fallback if it doesn't exist at all
                if (!langName.equalsIgnoreCase("EN")) {
                    plugin.getLogger().warning("Language file " + langName + ".yml not found! Falling back to EN.yml");
                    this.langName = "EN";
                    langFile = new File(langFolder, "EN.yml");
                    if (!langFile.exists()) {
                        plugin.saveResource("languages/EN.yml", false);
                    }
                }
            }
        }

        this.langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Load defaults from jar if available
        InputStream defLangStream = plugin.getResource("languages/" + langName + ".yml");
        if (defLangStream != null) {
            this.langConfig.setDefaults(
                    YamlConfiguration.loadConfiguration(new InputStreamReader(defLangStream, StandardCharsets.UTF_8)));
        }
    }

    public String getMessage(String key) {
        String message = langConfig.getString(key);
        if (message == null)
            return "Missing key: " + key;

        String prefix = langConfig.getString("prefix", "");
        return ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
    }

    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }

    public String getMessage(Player player, String key, String... placeholders) {
        String message = getMessage(key, placeholders);
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }

    public String getMessage(CommandSender sender, String key, String... placeholders) {
        if (sender instanceof Player) {
            return getMessage((Player) sender, key, placeholders);
        }
        return getMessage(key, placeholders);
    }
}
