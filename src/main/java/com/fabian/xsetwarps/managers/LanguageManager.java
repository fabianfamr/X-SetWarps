package com.fabian.xsetwarps.managers;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.utils.ColorUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LanguageManager {
    private final XSetWarps plugin;
    private FileConfiguration messagesConfig;
    private String currentLang;

    public LanguageManager(XSetWarps plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        // Get language from config (use lowercase for file names)
        String lang = plugin.getConfig().getString("language", "en").toLowerCase();

        // Ensure the messages folder exists
        File messagesFolder = new File(plugin.getDataFolder(), "messages");
        if (!messagesFolder.exists()) {
            messagesFolder.mkdirs();
        }

        // Export default messages files if they don't exist
        String[] defaultLangs = { "en", "es", "pt", "ja", "ru" };
        for (String l : defaultLangs) {
            File file = new File(messagesFolder, l + ".yml");
            if (!file.exists()) {
                try {
                    plugin.saveResource("messages/" + l + ".yml", false);
                } catch (Exception ignored) {
                }
            }
        }

        // Load the messages file
        File messagesFile = new File(messagesFolder, lang + ".yml");

        // If the language file doesn't exist, fallback to English
        if (!messagesFile.exists()) {
            if (!lang.equalsIgnoreCase("en")) {
                plugin.getLogger().warning("Language file " + lang + ".yml not found! Falling back to EN.");
                lang = "en";
                messagesFile = new File(messagesFolder, "en.yml");
                if (!messagesFile.exists()) {
                    plugin.saveResource("messages/en.yml", false);
                }
            }
        }

        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        this.currentLang = lang.toUpperCase();

        // Load defaults from JAR if available
        InputStream defStream = plugin.getResource("messages/" + lang + ".yml");
        if (defStream != null) {
            this.messagesConfig.setDefaults(
                    YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8)));
        }
    }

    /**
     * Get available language files
     */
    public List<String> getAvailableLanguages() {
        List<String> langs = new ArrayList<>();
        File messagesFolder = new File(plugin.getDataFolder(), "messages");
        if (messagesFolder.exists() && messagesFolder.isDirectory()) {
            File[] files = messagesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    langs.add(name.replace(".yml", ""));
                }
            }
        }
        return langs;
    }

    /**
     * Change the language
     */
    public boolean setLanguage(String lang) {
        String newLang = lang.toLowerCase();
        List<String> available = getAvailableLanguages();

        // Convert available languages to lowercase for comparison
        List<String> availableLower = new ArrayList<>();
        for (String l : available) {
            availableLower.add(l.toLowerCase());
        }

        if (!availableLower.contains(newLang)) {
            return false;
        }

        // Update config
        plugin.getConfig().set("language", newLang);
        plugin.saveConfig();

        // Reload messages
        this.currentLang = newLang.toUpperCase();
        loadLanguage();

        return true;
    }

    public String getCurrentLanguage() {
        return currentLang;
    }

    public String getMessage(String key) {
        String message = messagesConfig.getString(key);
        if (message == null) {
            return "Missing key: " + key;
        }

        // Get prefix from language file, fallback to config.yml
        String prefix = messagesConfig.getString("prefix", plugin.getConfig().getString("prefix", ""));
        String translated = ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
        
        // Apply advanced colors (hex)
        return ColorUtils.translateColors(translated);
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

    public String getMessage(CommandSender sender, String key, String... placeholders) {
        if (sender instanceof Player) {
            return getMessage((Player) sender, key, placeholders);
        }
        return getMessage(key, placeholders);
    }

    public String getMessage(Player player, String key, String... placeholders) {
        String message = getMessage(key, placeholders);
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }
}
