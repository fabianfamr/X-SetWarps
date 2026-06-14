package com.fabian.xsetwarps.managers;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.utils.ColorUtils;
import com.fabian.xsetwarps.utils.DebugLogger;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LanguageManager {
    private final XSetWarps plugin;
    private FileConfiguration messagesConfig;
    private String currentLang;

    public LanguageManager(XSetWarps plugin) {
        this.plugin = plugin;
        DebugLogger.debug("LanguageManager", "Constructing LanguageManager...");
        loadLanguage();
    }

    public void loadLanguage() {
        DebugLogger.debug("LanguageManager", "loadLanguage() called");
        // Reload config without triggering migration logic again
        plugin.reloadConfig();

        // Get language from config (use lowercase for file names)
        String lang = plugin.getConfig().getString("language", "en").toLowerCase();
        DebugLogger.debug("LanguageManager", "Configured language: " + lang);

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
                plugin.logWarning("Language file " + lang + ".yml not found! Falling back to EN.");
                lang = "en";
                messagesFile = new File(messagesFolder, "en.yml");
                if (!messagesFile.exists()) {
                    plugin.saveResource("messages/en.yml", false);
                }
            }
        }

        try {
            this.messagesConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(messagesFile), StandardCharsets.UTF_8));
        } catch (java.io.FileNotFoundException e) {
            this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        }
        this.currentLang = lang.toUpperCase();
        DebugLogger.debug("LanguageManager", "Loaded language file: " + lang + ".yml");

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
        DebugLogger.debug("LanguageManager", "Changing language to: " + lang);
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
        DebugLogger.debug("LanguageManager", "Language changed successfully to: " + newLang);

        return true;
    }

    public String getCurrentLanguage() {
        return currentLang;
    }

    public String getMessage(String key) {
        String message = messagesConfig.getString(key);
        if (message == null) {
            DebugLogger.debug("LanguageManager", "Missing message key: " + key);
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

    /**
     * Extracts a resource from the JAR and overwrites the destination file.
     * Used by force-reset to guarantee a fresh copy regardless of disk state.
     */
    private void extractResource(String resourcePath, File destination) {
        try (InputStream in = plugin.getResource(resourcePath);
             OutputStream out = new FileOutputStream(destination)) {
            if (in == null) {
                plugin.logWarning("Resource not found in JAR: " + resourcePath);
                return;
            }
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            plugin.logWarning("Could not extract resource: " + resourcePath + " - " + e.getMessage());
        }
    }

    /**
     * Force-updates a specific language file from JAR defaults (adds missing keys),
     * then reloads into memory if it is the currently active language.
     *
     * @param langCode language code (e.g. "en", "es")
     * @return true if the active language was reloaded
     */
    public boolean forceReloadMessages(String langCode) {
        String fileName = langCode.endsWith(".yml") ? langCode : langCode + ".yml";
        File messagesFolder = new File(plugin.getDataFolder(), "messages");
        File diskFile = new File(messagesFolder, fileName);

        if (!diskFile.exists()) {
            try {
                plugin.saveResource("messages/" + fileName, false);
            } catch (Exception ignored) {
            }
        }

        com.fabian.xsetwarps.utils.ConfigUpdater.update(plugin, "messages/" + fileName, diskFile);

        if (currentLang.equalsIgnoreCase(langCode)) {
            loadLanguage();
            return true;
        }
        return false;
    }

    /**
     * Force-updates ALL language files found on disk (adds missing keys),
     * then reloads the active language into memory.
     *
     * @return the number of language files that were processed
     */
    public int forceReloadAllMessages() {
        List<String> available = getAvailableLanguages();
        for (String lang : available) {
            String fileName = lang.endsWith(".yml") ? lang : lang + ".yml";
            File messagesFolder = new File(plugin.getDataFolder(), "messages");
            File diskFile = new File(messagesFolder, fileName);

            if (diskFile.exists()) {
                com.fabian.xsetwarps.utils.ConfigUpdater.update(plugin, "messages/" + fileName, diskFile);
            } else {
                try {
                    plugin.saveResource("messages/" + fileName, false);
                } catch (Exception ignored) {
                }
            }
        }
        loadLanguage();
        return available.size();
    }

    /**
     * Force-reset (overwrite) a specific language file from JAR defaults,
     * then reloads into memory if it is the currently active language.
     *
     * @param langCode language code (e.g. "en", "es")
     * @return true if the active language was reloaded
     */
    public boolean forceResetMessages(String langCode) {
        String fileName = langCode.endsWith(".yml") ? langCode : langCode + ".yml";
        File messagesFolder = new File(plugin.getDataFolder(), "messages");
        File diskFile = new File(messagesFolder, fileName);

        if (diskFile.exists()) {
            diskFile.delete();
        }
        extractResource("messages/" + fileName, diskFile);

        if (currentLang.equalsIgnoreCase(langCode)) {
            loadLanguage();
            return true;
        }
        return false;
    }

    /**
     * Force-reset (overwrite) ALL default language files from JAR,
     * then reloads the active language into memory.
     *
     * @return the number of language files that were reset
     */
    public int forceResetAllMessages() {
        String[] defaults = { "en", "es", "pt", "ja", "ru" };
        File messagesFolder = new File(plugin.getDataFolder(), "messages");

        for (String lang : defaults) {
            File diskFile = new File(messagesFolder, lang + ".yml");
            extractResource("messages/" + lang + ".yml", diskFile);
        }

        loadLanguage();
        return defaults.length;
    }
}
