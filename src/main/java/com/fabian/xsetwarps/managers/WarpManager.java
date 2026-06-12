package com.fabian.xsetwarps.managers;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.models.Warp;
import com.fabian.xsetwarps.utils.DebugLogger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages warps from multiple files (identifiers).
 * Each file in warps/ folder represents a group of warps.
 * Example: warps.yml, crates.yml, spawns.yml
 */
public class WarpManager {
    private final XSetWarps plugin;
    private final Map<String, Map<String, Warp>> warpsByFile; // fileName -> (warpName -> Warp)
    private final Map<String, FileConfiguration> configsByFile;
    private final Set<String> registeredPermissions = new HashSet<>();
    private final Object warpLock = new Object();
    
    public WarpManager(XSetWarps plugin) {
        this.plugin = plugin;
        this.warpsByFile = new HashMap<>();
        this.configsByFile = new HashMap<>();
        DebugLogger.debug("WarpManager", "Constructing WarpManager, loading warps...");
        loadAllWarps();
    }

    /**
     * Load all warp files from the warps folder
     */
    public void loadAllWarps() {
        DebugLogger.debug("WarpManager", "loadAllWarps() called");
        // Remove previously registered dynamic permissions before reloading
        unregisterPermissions();

        warpsByFile.clear();
        configsByFile.clear();
        
        File warpsFolder = new File(plugin.getDataFolder(), "warps");
        if (!warpsFolder.exists()) {
            warpsFolder.mkdirs();
            DebugLogger.debug("WarpManager", "Created warps/ directory");
        }
        
        // Load all yml files from warps folder
        File[] files = warpsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            DebugLogger.debug("WarpManager", "Found " + files.length + " warp file(s) in warps/");
            for (File file : files) {
                String identifier = file.getName().replace(".yml", "");
                DebugLogger.debug("WarpManager", "Loading warp file: " + identifier + ".yml");
                loadWarpsFromFile(identifier);
            }
        } else {
            DebugLogger.debug("WarpManager", "No warp files found in warps/");
        }
        
        // Create default warps.yml if it doesn't exist
        if (!warpsByFile.containsKey("warps")) {
            File defaultFile = new File(warpsFolder, "warps.yml");
            if (!defaultFile.exists()) {
                plugin.saveResource("warps/warps.yml", false);
            }
            loadWarpsFromFile("warps");
        }

        // Register all warp permissions so LuckPerms and other plugins can see them
        registerPermissions();
        DebugLogger.debug("WarpManager", "All warps loaded, total count: " + getTotalWarpCount());
    }

    /**
     * Register all dynamic warp permissions with Bukkit.
     * This allows LuckPerms and other permission plugins to discover them.
     */
    private void registerPermissions() {
        boolean perWarpPermission = plugin.getConfig().getBoolean("warps.per-warp-permission", false);

        for (Warp warp : getAllWarps()) {
            // Register custom permission (e.g. "warp.box", "warp.vip")
            if (warp.getPermission() != null && !warp.getPermission().trim().isEmpty()) {
                registerPermission(warp.getPermission());
            }

            // Register per-warp permission (e.g. "xsetwarps.warp.box") if enabled
            if (perWarpPermission) {
                registerPermission("xsetwarps.warp." + warp.getName().toLowerCase());
            }
        }
    }

    /**
     * Register a single permission with Bukkit if not already registered.
     */
    private void registerPermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) return;

        try {
            if (Bukkit.getPluginManager().getPermission(permission) == null) {
                Bukkit.getPluginManager().addPermission(
                        new Permission(permission, PermissionDefault.FALSE));
            }
            registeredPermissions.add(permission);
        } catch (Exception e) {
            plugin.logWarning("Failed to register permission: " + permission);
        }
    }

    /**
     * Remove all dynamically registered permissions (for reload).
     */
    private void unregisterPermissions() {
        for (String perm : registeredPermissions) {
            try {
                Permission existing = Bukkit.getPluginManager().getPermission(perm);
                if (existing != null) {
                    Bukkit.getPluginManager().removePermission(existing);
                }
            } catch (Exception ignored) {
                // Permission may already be removed
            }
        }
        registeredPermissions.clear();
    }

    /**
     * Register a permission for a newly created warp.
     * Call this after saving a new warp.
     */
    public void registerWarpPermission(Warp warp) {
        if (warp.getPermission() != null && !warp.getPermission().trim().isEmpty()) {
            registerPermission(warp.getPermission());
        }
        if (plugin.getConfig().getBoolean("warps.per-warp-permission", false)) {
            registerPermission("xsetwarps.warp." + warp.getName().toLowerCase());
        }
    }
    
    /**
     * Load warps from a specific file
     */
    private void loadWarpsFromFile(String identifier) {
        File warpsFile = new File(plugin.getDataFolder(), "warps/" + identifier + ".yml");
        
        if (!warpsFile.exists()) {
            warpsFile.getParentFile().mkdirs();
            // Don't save default resource for non-warps files
            if (identifier.equals("warps")) {
                plugin.saveResource("warps/warps.yml", false);
            }
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
        configsByFile.put(identifier, config);
        
        Map<String, Warp> warpsInFile = new HashMap<>();
        int count = 0;
        
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            String worldName  = section.getString("world", "world");
            double x          = section.getDouble("x");
            double y          = section.getDouble("y");
            double z          = section.getDouble("z");
            float yaw         = (float) section.getDouble("yaw");
            float pitch       = (float) section.getDouble("pitch");
            String description = section.getString("description", "");
            String createdBy  = section.getString("created-by", "unknown");
            long createdAt    = section.getLong("created-at", System.currentTimeMillis());
            String permission = section.getString("permission", "");
            int cooldown = section.getInt("cooldown", -1); // -1 = use global default

            Warp warp = new Warp(key, worldName, x, y, z, yaw, pitch, description, createdBy, createdAt, identifier);
            warp.setPermission(permission);
            warp.setCooldown(cooldown);
            warpsInFile.put(key.toLowerCase(), warp);
            count++;
        }
        
        warpsByFile.put(identifier, warpsInFile);
        DebugLogger.debug("WarpManager", "Loaded " + count + " warp(s) from " + identifier + ".yml");
    }

    /**
     * Save a warp to its respective file based on identifier
     */
    public boolean saveWarp(Warp warp) {
        synchronized (warpLock) {
            DebugLogger.debug("WarpManager", "Saving warp: " + warp.getName() + " (category: " + warp.getCategory() + ")");
            String identifier = warp.getCategory();
            
            // Check max-warps limit (global)
            int maxWarps = plugin.getConfig().getInt("warps.max-warps", -1);
            if (maxWarps > 0 && getTotalWarpCount() >= maxWarps) {
                DebugLogger.debug("WarpManager", "Max warps limit reached (" + maxWarps + "), cannot save warp: " + warp.getName());
                return false;
            }

            // Ensure the file exists
            Map<String, Warp> warpsInFile = warpsByFile.computeIfAbsent(identifier, k -> new HashMap<>());
            warpsInFile.put(warp.getName().toLowerCase(), warp);
            
            // Save to config
            FileConfiguration config = configsByFile.computeIfAbsent(identifier, k -> {
                File file = new File(plugin.getDataFolder(), "warps/" + identifier + ".yml");
                return YamlConfiguration.loadConfiguration(file);
            });
            
            String path = warp.getName();
            config.set(path + ".world",       warp.getWorldName());
            config.set(path + ".x",           warp.getX());
            config.set(path + ".y",           warp.getY());
            config.set(path + ".z",           warp.getZ());
            config.set(path + ".yaw",         warp.getYaw());
            config.set(path + ".pitch",       warp.getPitch());
            config.set(path + ".category",    warp.getCategory());
            if (warp.getPermission() != null && !warp.getPermission().isEmpty()) {
                config.set(path + ".permission", warp.getPermission());
            } else {
                config.set(path + ".permission", null);
            }
            if (warp.getCooldown() >= 0) {
                config.set(path + ".cooldown", warp.getCooldown());
            } else {
                config.set(path + ".cooldown", null); // Don't write if using default
            }
            config.set(path + ".description", warp.getDescription());
            config.set(path + ".created-by",  warp.getCreatedBy());
            config.set(path + ".created-at",  warp.getCreatedAt());

            try {
                File file = new File(plugin.getDataFolder(), "warps/" + identifier + ".yml");
                config.save(file);
                // Register the permission so LuckPerms can see it immediately
                registerWarpPermission(warp);
                DebugLogger.debug("WarpManager", "Warp saved successfully: " + warp.getName());
            } catch (IOException e) {
                DebugLogger.debug("WarpManager", "Failed to save warp: " + warp.getName(), e);
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    /**
     * Delete a warp from its file
     */
    public void deleteWarp(String name) {
        synchronized (warpLock) {
            DebugLogger.debug("WarpManager", "Deleting warp: " + name);
            Warp warp = getWarp(name);
            if (warp == null) {
                DebugLogger.debug("WarpManager", "Warp not found for deletion: " + name);
                return;
            }
            
            String identifier = warp.getCategory();
            Map<String, Warp> warpsInFile = warpsByFile.get(identifier);
            if (warpsInFile != null) {
                warpsInFile.remove(name.toLowerCase());
            }
            
            FileConfiguration config = configsByFile.get(identifier);
            if (config != null) {
                // Use the original warp name (case-sensitive) to remove from YAML
                config.set(warp.getName(), null);
                try {
                    File file = new File(plugin.getDataFolder(), "warps/" + identifier + ".yml");
                    config.save(file);
                    DebugLogger.debug("WarpManager", "Warp deleted successfully: " + name);
                } catch (IOException e) {
                    DebugLogger.debug("WarpManager", "Failed to delete warp file entry: " + name, e);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get a warp by name (searches all files)
     */
    public Warp getWarp(String name) {
        String lowerName = name.toLowerCase();
        for (Map<String, Warp> warpsInFile : warpsByFile.values()) {
            Warp warp = warpsInFile.get(lowerName);
            if (warp != null) {
                DebugLogger.debug("WarpManager", "getWarp(\"" + name + "\") -> found in category: " + warp.getCategory());
                return warp;
            }
        }
        DebugLogger.debug("WarpManager", "getWarp(\"" + name + "\") -> not found");
        return null;
    }

    /**
     * Check if a warp exists
     */
    public boolean warpExists(String name) {
        return getWarp(name) != null;
    }

    /**
     * Get total warp count across all files
     */
    public int getTotalWarpCount() {
        int count = 0;
        for (Map<String, Warp> warpsInFile : warpsByFile.values()) {
            count += warpsInFile.size();
        }
        return count;
    }

    /**
     * Get all warp names (across all files)
     */
    public Set<String> getWarpNames() {
        Set<String> names = new HashSet<>();
        for (Map<String, Warp> warpsInFile : warpsByFile.values()) {
            names.addAll(warpsInFile.keySet());
        }
        return names;
    }

    /**
     * Get warps from a specific file (identifier)
     */
    public Map<String, Warp> getWarpsByIdentifier(String identifier) {
        return warpsByFile.getOrDefault(identifier, new HashMap<>());
    }

    /**
     * Get all available identifiers (file names without .yml)
     */
    public Set<String> getIdentifiers() {
        return new HashSet<>(warpsByFile.keySet());
    }

    /**
     * Get all warps across all files
     */
    public List<Warp> getAllWarps() {
        List<Warp> allWarps = new ArrayList<>();
        for (Map<String, Warp> warpsInFile : warpsByFile.values()) {
            allWarps.addAll(warpsInFile.values());
        }
        return allWarps;
    }

    /**
     * Get warps grouped by identifier
     */
    public Map<String, List<Warp>> getWarpsGroupedByIdentifier() {
        Map<String, List<Warp>> grouped = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Warp>> entry : warpsByFile.entrySet()) {
            grouped.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
        }
        return grouped;
    }

    /**
     * Export warps from a specific identifier (or all) to a YAML file.
     * Returns the number of exported warps.
     */
    public int exportWarps(String identifier, String fileName) {
        List<Warp> warpsToExport;
        if (identifier != null && !identifier.isEmpty()) {
            Map<String, Warp> map = warpsByFile.get(identifier);
            warpsToExport = map != null ? new ArrayList<>(map.values()) : new ArrayList<>();
        } else {
            warpsToExport = getAllWarps();
        }

        YamlConfiguration export = new YamlConfiguration();
        for (Warp warp : warpsToExport) {
            String path = warp.getName();
            export.set(path + ".world", warp.getWorldName());
            export.set(path + ".x", warp.getX());
            export.set(path + ".y", warp.getY());
            export.set(path + ".z", warp.getZ());
            export.set(path + ".yaw", warp.getYaw());
            export.set(path + ".pitch", warp.getPitch());
            export.set(path + ".description", warp.getDescription());
            export.set(path + ".created-by", warp.getCreatedBy());
            export.set(path + ".created-at", warp.getCreatedAt());
            export.set(path + ".category", warp.getCategory());
            if (warp.getPermission() != null && !warp.getPermission().isEmpty()) {
                export.set(path + ".permission", warp.getPermission());
            }
            if (warp.getCooldown() >= 0) {
                export.set(path + ".cooldown", warp.getCooldown());
            }
        }

        File exportDir = new File(plugin.getDataFolder(), "exports");
        if (!exportDir.exists()) exportDir.mkdirs();

        String actualFileName = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        File file = new File(exportDir, actualFileName);
        try {
            export.save(file);
            return warpsToExport.size();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Import warps from an export YAML file.
     * Returns the number of imported warps.
     */
    public int importWarps(String fileName) {
        String actualFileName = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        File file = new File(plugin.getDataFolder(), "exports/" + actualFileName);

        if (!file.exists()) {
            file = new File(plugin.getDataFolder(), "warps/" + actualFileName);
        }

        if (!file.exists()) return -1;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int count = 0;

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            String worldName = section.getString("world", "world");
            final String wnCheck = worldName;
            if (!Bukkit.getWorlds().stream().anyMatch(w -> w.getName().equalsIgnoreCase(wnCheck))) {
                worldName = Bukkit.getWorlds().get(0).getName();
            }
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            float yaw = (float) section.getDouble("yaw");
            float pitch = (float) section.getDouble("pitch");
            String description = section.getString("description", "");
            String createdBy = section.getString("created-by", "imported");
            long createdAt = section.getLong("created-at", System.currentTimeMillis());
            String category = section.getString("category", Warp.DEFAULT_IDENTIFIER);
            String permission = section.getString("permission", "");
            int cooldown = section.getInt("cooldown", -1);

            Warp warp = new Warp(key, worldName, x, y, z, yaw, pitch, description, createdBy, createdAt, category);
            warp.setPermission(permission);
            warp.setCooldown(cooldown);

            if (!warpExists(key)) {
                saveWarp(warp);
                count++;
            }
        }

        return count;
    }

    /**
     * Import warps from Essentials plugin.
     * Returns the number of imported warps, or -2 if Essentials not found.
     */
    public int importEssentialsWarps() {
        if (Bukkit.getPluginManager().getPlugin("Essentials") == null) return -2;

        File essentialsWarps = new File("plugins/Essentials/warps.yml");
        if (!essentialsWarps.exists()) return -1;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(essentialsWarps);
        int count = 0;

        for (String key : config.getKeys(false)) {
            // Essentials stores location as string: "world:x:y:z:yaw:pitch"
            String locStr = config.getString(key);
            if (locStr == null || locStr.isEmpty()) continue;

            String[] parts = locStr.split(":");
            if (parts.length < 5) continue;

            try {
                String worldName = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                float yaw = Float.parseFloat(parts[4]);
                float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;

                final String wnCheck = worldName;
                if (!Bukkit.getWorlds().stream().anyMatch(w -> w.getName().equalsIgnoreCase(wnCheck))) {
                    worldName = Bukkit.getWorlds().get(0).getName();
                }

                Warp warp = new Warp(key, worldName, x, y, z, yaw, pitch, "", "Essentials", System.currentTimeMillis(), Warp.DEFAULT_IDENTIFIER);

                if (!warpExists(key)) {
                    saveWarp(warp);
                    count++;
                }
            } catch (NumberFormatException e) {
                // Skip malformed entries
            }
        }

        return count;
    }

    /**
     * Import warps from a generic plugin warp file.
     * Supports multiple formats: Essentials (colon-separated), CMI (section-based),
     * and standard X-SetWarps export format.
     * Returns the number of imported warps, or -1 if file not found, -2 if plugin not installed.
     */
    public int importPluginWarps(String pluginName, String filePath) {
        if (Bukkit.getPluginManager().getPlugin(pluginName) == null) return -2;

        File file = new File(filePath);
        if (!file.exists()) return -1;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int count = 0;

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            String worldName;
            double x, y, z;
            float yaw = 0, pitch = 0;

            try {
                if (section != null && (section.isSet("X") || section.isSet("x"))) {
                    // Section-based format (CMI-style)
                    worldName = section.getString("World", section.getString("world", "world"));
                    x = section.getDouble("X", section.getDouble("x", 0));
                    y = section.getDouble("Y", section.getDouble("y", 0));
                    z = section.getDouble("Z", section.getDouble("z", 0));
                    yaw = (float) section.getDouble("Yaw", section.getDouble("yaw", 0));
                    pitch = (float) section.getDouble("Pitch", section.getDouble("pitch", 0));
                } else {
                    // Colon-separated string format (Essentials-style)
                    String locStr = config.getString(key);
                    if (locStr == null || locStr.isEmpty()) continue;
                    String[] parts = locStr.split(":");
                    if (parts.length < 4) continue;
                    worldName = parts[0];
                    x = Double.parseDouble(parts[1]);
                    y = Double.parseDouble(parts[2]);
                    z = Double.parseDouble(parts[3]);
                    yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
                    pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;
                }

                final String wnCheck = worldName;
                if (!Bukkit.getWorlds().stream().anyMatch(w -> w.getName().equalsIgnoreCase(wnCheck))) {
                    worldName = Bukkit.getWorlds().get(0).getName();
                }

                Warp warp = new Warp(key, worldName, x, y, z, yaw, pitch, "", pluginName,
                        System.currentTimeMillis(), Warp.DEFAULT_IDENTIFIER);

                if (!warpExists(key)) {
                    saveWarp(warp);
                    count++;
                }
            } catch (NumberFormatException e) {
                plugin.logWarning("Skipping malformed warp entry: " + key);
            }
        }

        return count;
    }

    /**
     * Auto-detect and import warps from all known installed warp plugins.
     * Returns a map of plugin name -> number of warps imported.
     */
    public Map<String, Integer> importFromAllPlugins() {
        Map<String, Integer> results = new LinkedHashMap<>();

        // Essentials
        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            int count = importEssentialsWarps();
            results.put("Essentials", count);
        }

        // CMI
        if (Bukkit.getPluginManager().getPlugin("CMI") != null) {
            int count = importCMIWarps();
            results.put("CMI", count);
        }

        return results;
    }

    /**
     * Import warps from CMI plugin.
     * Returns the number of imported warps, or -2 if CMI not found.
     */
    public int importCMIWarps() {
        if (Bukkit.getPluginManager().getPlugin("CMI") == null) return -2;

        File cmiWarps = new File("plugins/CMI/warps.yml");
        if (!cmiWarps.exists()) return -1;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(cmiWarps);
        int count = 0;

        // CMI stores warps similar to Essentials
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            String worldName = section.getString("World", section.getString("world", "world"));
            double x = section.getDouble("X", section.getDouble("x", 0));
            double y = section.getDouble("Y", section.getDouble("y", 0));
            double z = section.getDouble("Z", section.getDouble("z", 0));
            float yaw = (float) section.getDouble("Yaw", section.getDouble("yaw", 0));
            float pitch = (float) section.getDouble("Pitch", section.getDouble("pitch", 0));

            final String wnCheck = worldName;
            if (!Bukkit.getWorlds().stream().anyMatch(w -> w.getName().equalsIgnoreCase(wnCheck))) {
                worldName = Bukkit.getWorlds().get(0).getName();
            }

            Warp warp = new Warp(key, worldName, x, y, z, yaw, pitch, "", "CMI", System.currentTimeMillis(), Warp.DEFAULT_IDENTIFIER);

            if (!warpExists(key)) {
                saveWarp(warp);
                count++;
            }
        }

        return count;
    }
}
