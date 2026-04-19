package com.fabian.xsetwarps.managers;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.model.Warp;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WarpManager {
    private final XSetWarps plugin;
    private final File warpsFile;
    private FileConfiguration warpsConfig;
    private final Map<String, Warp> warps;

    public WarpManager(XSetWarps plugin) {
        this.plugin = plugin;
        this.warps = new HashMap<>();
        this.warpsFile = new File(plugin.getDataFolder(), "warps/warps.yml");
        loadWarps();
    }

    public void loadWarps() {
        if (!warpsFile.exists()) {
            warpsFile.getParentFile().mkdirs();
            plugin.saveResource("warps/warps.yml", false);
        }
        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
        warps.clear();

        for (String key : warpsConfig.getKeys(false)) {
            ConfigurationSection section = warpsConfig.getConfigurationSection(key);
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

            Warp warp = new Warp(key, worldName, x, y, z, yaw, pitch, description, createdBy, createdAt);
            warps.put(key.toLowerCase(), warp);
        }
    }

    public boolean saveWarp(Warp warp) {
        // Check max-warps limit
        int maxWarps = plugin.getConfig().getInt("warps.max-warps", -1);
        if (maxWarps > 0 && !warps.containsKey(warp.getName().toLowerCase()) && warps.size() >= maxWarps) {
            return false; // limit reached
        }

        warps.put(warp.getName().toLowerCase(), warp);
        String path = warp.getName();

        warpsConfig.set(path + ".world",       warp.getWorldName());
        warpsConfig.set(path + ".x",           warp.getX());
        warpsConfig.set(path + ".y",           warp.getY());
        warpsConfig.set(path + ".z",           warp.getZ());
        warpsConfig.set(path + ".yaw",         warp.getYaw());
        warpsConfig.set(path + ".pitch",       warp.getPitch());
        warpsConfig.set(path + ".description", warp.getDescription());
        warpsConfig.set(path + ".created-by",  warp.getCreatedBy());
        warpsConfig.set(path + ".created-at",  warp.getCreatedAt());

        try {
            warpsConfig.save(warpsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void deleteWarp(String name) {
        warps.remove(name.toLowerCase());
        warpsConfig.set(name, null);
        try {
            warpsConfig.save(warpsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Warp getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public boolean warpExists(String name) {
        return warps.containsKey(name.toLowerCase());
    }

    public int getWarpCount() {
        return warps.size();
    }

    public Set<String> getWarpNames() {
        return warps.keySet();
    }
}
