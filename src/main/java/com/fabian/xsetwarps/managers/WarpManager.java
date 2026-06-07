package com.fabian.xsetwarps.managers;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.model.Warp;

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
    
    public WarpManager(XSetWarps plugin) {
        this.plugin = plugin;
        this.warpsByFile = new HashMap<>();
        this.configsByFile = new HashMap<>();
        loadAllWarps();
    }

    /**
     * Load all warp files from the warps folder
     */
    public void loadAllWarps() {
        // Remove previously registered dynamic permissions before reloading
        unregisterPermissions();

        warpsByFile.clear();
        configsByFile.clear();
        
        File warpsFolder = new File(plugin.getDataFolder(), "warps");
        if (!warpsFolder.exists()) {
            warpsFolder.mkdirs();
        }
        
        // Load all yml files from warps folder
        File[] files = warpsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String identifier = file.getName().replace(".yml", "");
                loadWarpsFromFile(identifier);
            }
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
            plugin.getLogger().warning("Failed to register permission: " + permission);
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

            Warp warp = new Warp(key, worldName, x, y, z, yaw, pitch, description, createdBy, createdAt, identifier);
            warp.setPermission(permission);
            warpsInFile.put(key.toLowerCase(), warp);
        }
        
        warpsByFile.put(identifier, warpsInFile);
    }

    /**
     * Save a warp to its respective file based on identifier
     */
    public boolean saveWarp(Warp warp) {
        String identifier = warp.getCategory();
        
        // Check max-warps limit (global)
        int maxWarps = plugin.getConfig().getInt("warps.max-warps", -1);
        if (maxWarps > 0 && getTotalWarpCount() >= maxWarps) {
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
        if (warp.getPermission() != null && !warp.getPermission().isEmpty()) {
            config.set(path + ".permission", warp.getPermission());
        } else {
            config.set(path + ".permission", "");
        }
        config.set(path + ".description", warp.getDescription());
        config.set(path + ".created-by",  warp.getCreatedBy());
        config.set(path + ".created-at",  warp.getCreatedAt());

        try {
            File file = new File(plugin.getDataFolder(), "warps/" + identifier + ".yml");
            config.save(file);
            // Register the permission so LuckPerms can see it immediately
            registerWarpPermission(warp);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Delete a warp from its file
     */
    public void deleteWarp(String name) {
        Warp warp = getWarp(name);
        if (warp == null) return;
        
        String identifier = warp.getCategory();
        Map<String, Warp> warpsInFile = warpsByFile.get(identifier);
        if (warpsInFile != null) {
            warpsInFile.remove(name.toLowerCase());
        }
        
        FileConfiguration config = configsByFile.get(identifier);
        if (config != null) {
            config.set(warp.getName(), null);
            try {
                File file = new File(plugin.getDataFolder(), "warps/" + identifier + ".yml");
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
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
            if (warp != null) return warp;
        }
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
}
