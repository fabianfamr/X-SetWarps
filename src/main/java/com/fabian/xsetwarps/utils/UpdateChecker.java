package com.fabian.xsetwarps.utils;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final XSetWarps plugin;
    private final int resourceId;
    private String latestVersion;
    private boolean updateAvailable;

    public UpdateChecker(XSetWarps plugin) {
        this.plugin = plugin;
        this.resourceId = 132480; // Spigot Resource ID for X-SetWarps
        this.updateAvailable = false;
    }

    public void checkForUpdates() {
        checkForUpdates(null);
    }

    public void checkForUpdates(CommandSender sender) {
        SchedulerUtils.runTaskAsync(plugin, () -> {
            HttpURLConnection connection = null;
            try {
                String current = plugin.getDescription().getVersion();

                // Spigot API for resource versions
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Fabian/X-SetWarps/" + current);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String version = reader.readLine();
                reader.close();

                this.latestVersion = version;
                LanguageManager lang = plugin.getLanguageManager();

                if (latestVersion != null && isNewer(current, latestVersion)) {
                    this.updateAvailable = true;

                    if (sender != null) {
                        sender.sendMessage(lang.getMessage("update-available", "%current%", current, "%latest%", latestVersion));
                        sender.sendMessage(lang.getMessage("update-download", "%url%", getDownloadUrl()));
                    } else {
                        SchedulerUtils.runTask(plugin, () -> {
                            Bukkit.getConsoleSender()
                                    .sendMessage(lang.getMessage("update-available", "%current%", current, "%latest%", latestVersion));
                            Bukkit.getConsoleSender().sendMessage(lang.getMessage("update-download", "%url%", getDownloadUrl()));
                        });
                    }
                } else {
                    if (sender != null) {
                        sender.sendMessage(lang.getMessage("update-current"));
                    } else {
                        SchedulerUtils.runTask(plugin, () -> {
                            Bukkit.getConsoleSender().sendMessage(lang.getMessage("update-current"));
                        });
                    }
                }

            } catch (Exception e) {
                if (sender != null) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("update-error"));
                } else {
                    SchedulerUtils.runTask(plugin, () -> {
                        Bukkit.getConsoleSender().sendMessage(plugin.getLanguageManager().getMessage("update-error"));
                    });
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getDownloadUrl() {
        return "https://www.spigotmc.org/resources/" + resourceId + "/";
    }

    private boolean isNewer(String current, String latest) {
        if (current == null || latest == null) return false;
        String[] currentParts = current.replace("v", "").split("\\.");
        String[] latestParts = latest.replace("v", "").split("\\.");
        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = 0;
            int latestPart = 0;
            try {
                currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i].replaceAll("[^0-9].*", "")) : 0;
            } catch (NumberFormatException e) {
                currentPart = 0;
            }
            try {
                latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i].replaceAll("[^0-9].*", "")) : 0;
            } catch (NumberFormatException e) {
                latestPart = 0;
            }
            if (latestPart > currentPart)
                return true;
            if (latestPart < currentPart)
                return false;
        }
        return false;
    }
}