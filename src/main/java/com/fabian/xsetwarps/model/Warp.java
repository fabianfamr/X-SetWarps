package com.fabian.xsetwarps.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Warp {
    private final String name;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private String description;
    private String createdBy;
    private long createdAt;
    private String category; // Identifier for the warp file (e.g., "spawns", "crates")
    private String permission;
    private int cooldown; // Individual per-warp cooldown in seconds (-1 = use global default)

    // Default identifier for the main warps file
    public static final String DEFAULT_IDENTIFIER = "warps";

    // Constructor used when loading from YAML (full data)
    public Warp(String name, String worldName, double x, double y, double z, float yaw, float pitch,
                String description, String createdBy, long createdAt) {
        this(name, worldName, x, y, z, yaw, pitch, description, createdBy, createdAt, DEFAULT_IDENTIFIER);
    }

    // Constructor used when loading from YAML with category
    public Warp(String name, String worldName, double x, double y, double z, float yaw, float pitch,
                String description, String createdBy, long createdAt, String category) {
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.description = description != null ? description : "";
        this.createdBy = createdBy != null ? createdBy : "unknown";
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.category = category;
        this.permission = "";
        this.cooldown = -1; // -1 means use global default from config
    }

    // Constructor used when creating a new warp in-game
    public Warp(String name, Location loc, String description, String creatorName) {
        this(name, loc, description, creatorName, DEFAULT_IDENTIFIER);
    }

    // Constructor used when creating a new warp in-game with category
    public Warp(String name, Location loc, String description, String creatorName, String category) {
        this.name = name;
        this.worldName = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
        this.description = description != null ? description : "";
        this.createdBy = creatorName;
        this.createdAt = System.currentTimeMillis();
        this.category = category;
        this.permission = "";
        this.cooldown = -1;
    }

    // Legacy constructor for backwards compatibility
    public Warp(String name, Location loc) {
        this(name, loc, "", "unknown");
    }

    // Legacy constructor for backwards compatibility (loading old warps.yml without new fields)
    public Warp(String name, String worldName, double x, double y, double z, float yaw, float pitch) {
        this(name, worldName, x, y, z, yaw, pitch, "", "unknown", System.currentTimeMillis(), DEFAULT_IDENTIFIER);
    }

    public String getName() { return name; }

    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getWorldName()  { return worldName; }
    public double getX()          { return x; }
    public double getY()          { return y; }
    public double getZ()          { return z; }
    public float getYaw()         { return yaw; }
    public float getPitch()       { return pitch; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description != null ? description : ""; }


    public String getCreatedBy() { return createdBy; }
    public long getCreatedAt()   { return createdAt; }

    // Getter and setter for identifier (file name without .yml)
    public String getCategory() { return category != null ? category : "default"; }
    public void setCategory(String category) { this.category = category; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission != null ? permission : ""; }

    /**
     * Returns the per-warp cooldown in seconds.
     * -1 means use the global default from config (teleport.cooldown).
     * 0 means no cooldown for this specific warp.
     * Any positive value overrides the global cooldown for this warp only.
     */
    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }

    /**
     * Returns the effective cooldown in seconds for this warp,
     * resolving -1 to the provided global default value.
     */
    public int getEffectiveCooldown(int globalDefault) {
        return cooldown < 0 ? globalDefault : cooldown;
    }
}