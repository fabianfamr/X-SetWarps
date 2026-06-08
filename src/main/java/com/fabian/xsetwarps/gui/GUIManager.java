package com.fabian.xsetwarps.gui;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.model.Warp;
import com.fabian.xsetwarps.utils.ColorUtils;

import com.cryptomorin.xseries.XMaterial;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class GUIManager {

    private final XSetWarps plugin;

    private String guiTitle;
    private int guiRows;

    private String fillerMaterial;
    private String fillerName;

    private int closeSlot;
    private String closeMaterial;
    private String closeTexture;
    private String closeName;
    private List<String> closeLore;

    private int prevPageSlot;
    private int nextPageSlot;

    private boolean usePlayerHeads;

    private boolean pageInfoEnabled;
    private int pageInfoSlot;
    private String pageInfoMaterial;
    private String pageInfoName;
    private List<String> pageInfoLore;

    private final Map<String, WarpItemConfig> predefinedItems = new LinkedHashMap<>();
    private final List<WarpItemConfig> staticItems = new ArrayList<>();

    private static final int ITEMS_PER_PAGE = 28;

    private final Map<UUID, Map<Integer, String>> openWarpSlots = new HashMap<>();

    private static class WarpItemConfig {
        final int slot;
        final String material;
        final String skullTexture;
        final String skullPlayer;
        final String name;
        final List<String> lore;

        WarpItemConfig(int slot, String material, String skullTexture, String skullPlayer,
                       String name, List<String> lore) {
            this.slot = slot;
            this.material = material;
            this.skullTexture = skullTexture;
            this.skullPlayer = skullPlayer;
            this.name = name;
            this.lore = lore != null ? lore : new ArrayList<>();
        }
    }

    public GUIManager(XSetWarps plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(guiFile);

        guiTitle = ColorUtils.translateColors(config.getString("gui.title", "&8Warps"));
        guiRows = config.getInt("gui.rows", 4);
        usePlayerHeads = config.getBoolean("gui.use-player-heads", false);

        fillerMaterial = config.getString("filler.material", "BLACK_STAINED_GLASS_PANE");
        fillerName = ColorUtils.translateColors(config.getString("filler.name", " "));

        closeSlot = config.getInt("buttons.close.slot", 31);
        closeMaterial = config.getString("buttons.close.material", "PLAYER_HEAD");
        closeTexture = config.getString("buttons.close.skull-texture", "");
        closeName = ColorUtils.translateColors(config.getString("buttons.close.name", "&fClose"));
        closeLore = translateLore(config.getStringList("buttons.close.lore"));

        prevPageSlot = config.getInt("navigation.prev-page.slot", 27);
        nextPageSlot = config.getInt("navigation.next-page.slot", 35);

        pageInfoEnabled = config.getBoolean("page-info.enabled", false);
        pageInfoSlot = config.getInt("page-info.slot", 31);
        pageInfoMaterial = config.getString("page-info.material", "PAPER");
        pageInfoName = config.getString("page-info.name", "&7Warps: &e%count%");
        pageInfoLore = translateLore(config.getStringList("page-info.lore"));

        predefinedItems.clear();
        staticItems.clear();
        ConfigurationSection warpItemsSection = config.getConfigurationSection("warp-items");
        if (warpItemsSection != null) {
            for (String key : warpItemsSection.getKeys(false)) {
                ConfigurationSection itemSection = warpItemsSection.getConfigurationSection(key);
                if (itemSection == null) continue;

                String warpName = itemSection.getString("warp");

                WarpItemConfig itemConfig = new WarpItemConfig(
                        itemSection.getInt("slot"),
                        itemSection.getString("material"),
                        itemSection.getString("skull-texture"),
                        itemSection.getString("skull-player"),
                        itemSection.getString("name"),
                        itemSection.getStringList("lore")
                );

                if (warpName == null || warpName.isEmpty()) {
                    staticItems.add(itemConfig);
                } else {
                    predefinedItems.put(warpName.toLowerCase(), itemConfig);
                }
            }
        }
    }

    public void openWarpsListGUI(Player player, int page) {
        if (page < 1) page = 1;

        List<Warp> allWarps = plugin.getWarpManager().getAllWarps();
        List<Warp> dynamicWarpsTotal = new ArrayList<>();
        for (Warp warp : allWarps) {
            if (predefinedItems.containsKey(warp.getName().toLowerCase())) continue;
            dynamicWarpsTotal.add(warp);
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) dynamicWarpsTotal.size() / ITEMS_PER_PAGE));
        if (page > totalPages) page = totalPages;

        int inventorySize = guiRows * 9;
        WarpGUIHolder holder = new WarpGUIHolder(page);
        Inventory inv = Bukkit.createInventory(holder, inventorySize, guiTitle);

        fillInventory(inv);

        Set<Integer> occupiedSlots = new HashSet<>();
        occupiedSlots.add(closeSlot);
        occupiedSlots.add(prevPageSlot);
        occupiedSlots.add(nextPageSlot);
        if (pageInfoEnabled) {
            occupiedSlots.add(pageInfoSlot);
        }

        Map<Integer, String> slotWarpMap = new HashMap<>();

        // Place ALL predefined items at their slots (always show regardless of warp existence)
        for (Map.Entry<String, WarpItemConfig> entry : predefinedItems.entrySet()) {
            WarpItemConfig config = entry.getValue();
            if (occupiedSlots.contains(config.slot)) continue;
            Warp warp = plugin.getWarpManager().getWarp(entry.getKey());
            ItemStack item = (warp != null) ? buildWarpItem(warp, config) : buildStaticItem(config);
            if (item != null) {
                inv.setItem(config.slot, item);
                occupiedSlots.add(config.slot);
                slotWarpMap.put(config.slot, entry.getKey());
            }
        }
        // Place static decorative items (warp: "")
        for (WarpItemConfig config : staticItems) {
            if (occupiedSlots.contains(config.slot)) continue;
            ItemStack item = buildStaticItem(config);
            if (item != null) {
                inv.setItem(config.slot, item);
                occupiedSlots.add(config.slot);
            }
        }

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, dynamicWarpsTotal.size());
        List<Warp> dynamicWarps = dynamicWarpsTotal.subList(startIndex, endIndex);

        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < inventorySize; i++) {
            if (!occupiedSlots.contains(i)) {
                availableSlots.add(i);
            }
        }

        for (int i = 0; i < dynamicWarps.size() && i < availableSlots.size(); i++) {
            Warp warp = dynamicWarps.get(i);
            int slot = availableSlots.get(i);
            ItemStack item = buildDynamicWarpItem(warp);
            inv.setItem(slot, item);
            slotWarpMap.put(slot, warp.getName());
        }

        addCloseButton(inv);

        if (pageInfoEnabled) {
            ItemStack pageInfoItem = buildPageInfoItem(allWarps.size());
            inv.setItem(pageInfoSlot, pageInfoItem);
        }

        if (totalPages > 1) {
            addPageNavigation(inv, page, totalPages);
        }

        openWarpSlots.put(player.getUniqueId(), slotWarpMap);
        player.openInventory(inv);
    }

    private void fillInventory(Inventory inv) {
        XMaterial xFiller = XMaterial.matchXMaterial(fillerMaterial).orElse(XMaterial.BLACK_STAINED_GLASS_PANE);
        ItemStack filler = xFiller.parseItem();
        if (filler != null) {
            ItemMeta meta = filler.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(fillerName);
                filler.setItemMeta(meta);
            }
        }
        if (filler != null) {
            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, filler.clone());
            }
        }
    }

    private void addCloseButton(Inventory inv) {
        ItemStack closeItem = buildSkullItem(closeMaterial, closeTexture, null);
        if (closeItem == null) {
            XMaterial xMat = XMaterial.matchXMaterial(closeMaterial).orElse(XMaterial.BARRIER);
            closeItem = xMat.parseItem();
        }
        if (closeItem != null) {
            ItemMeta meta = closeItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(closeName);
                meta.setLore(closeLore.isEmpty() ? null : closeLore);
                closeItem.setItemMeta(meta);
            }
        }
        if (closeItem != null) {
            inv.setItem(closeSlot, closeItem);
        }
    }

    private void addPageNavigation(Inventory inv, int currentPage, int totalPages) {
        if (currentPage > 1) {
            ItemStack prevItem = XMaterial.ARROW.parseItem();
            if (prevItem != null) {
                ItemMeta meta = prevItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ColorUtils.translateColors("&e&l\u00ab Previous Page"));
                    meta.setLore(Collections.singletonList(
                            ColorUtils.translateColors("&7Page " + (currentPage - 1) + " of " + totalPages)));
                    prevItem.setItemMeta(meta);
                }
                inv.setItem(prevPageSlot, prevItem);
            }
        }

        if (currentPage < totalPages) {
            ItemStack nextItem = XMaterial.ARROW.parseItem();
            if (nextItem != null) {
                ItemMeta meta = nextItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ColorUtils.translateColors("&e&lNext Page \u00bb"));
                    meta.setLore(Collections.singletonList(
                            ColorUtils.translateColors("&7Page " + (currentPage + 1) + " of " + totalPages)));
                    nextItem.setItemMeta(meta);
                }
                inv.setItem(nextPageSlot, nextItem);
            }
        }
    }

    private ItemStack buildWarpItem(Warp warp, WarpItemConfig preset) {
        String name = preset.name != null && !preset.name.isEmpty()
                ? ColorUtils.translateColors(preset.name)
                : ColorUtils.translateColors("&e" + warp.getName());

        List<String> lore = preset.lore != null && !preset.lore.isEmpty()
                ? translateLore(preset.lore)
                : new ArrayList<>();

        ItemStack item;

        if (isSkullMaterial(preset.material)) {
            item = buildSkullItem(preset.material, preset.skullTexture, preset.skullPlayer);
        } else {
            XMaterial xMat = XMaterial.matchXMaterial(preset.material).orElse(null);
            if (xMat == null) {
                xMat = XMaterial.ENDER_PEARL;
            }
            item = xMat.parseItem();
        }

        if (item == null) {
            item = XMaterial.ENDER_PEARL.parseItem();
        }
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack buildStaticItem(WarpItemConfig config) {
        if (isSkullMaterial(config.material)) {
            ItemStack item = buildSkullItem(config.material, config.skullTexture, config.skullPlayer);
            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ColorUtils.translateColors(config.name));
                    meta.setLore(translateLore(config.lore));
                    item.setItemMeta(meta);
                }
            }
            return item;
        }

        XMaterial xMat = XMaterial.matchXMaterial(config.material).orElse(null);
        if (xMat == null) return null;
        ItemStack item = xMat.parseItem();
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.translateColors(config.name));
            meta.setLore(translateLore(config.lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildDynamicWarpItem(Warp warp) {
        String name = ColorUtils.translateColors("&e" + warp.getName());
        List<String> lore = new ArrayList<>();
        if (warp.getDescription() != null && !warp.getDescription().isEmpty()) {
            lore.add(ColorUtils.translateColors("&7" + warp.getDescription()));
        }
        if (warp.getCreatedBy() != null && !warp.getCreatedBy().equals("unknown")) {
            lore.add(ColorUtils.translateColors("&7Created by: &f" + warp.getCreatedBy()));
        }
        lore.add(ColorUtils.translateColors("&7Click to teleport!"));

        ItemStack item;
        if (usePlayerHeads && warp.getCreatedBy() != null && !warp.getCreatedBy().equals("unknown")) {
            item = buildSkullItem("PLAYER_HEAD", null, warp.getCreatedBy());
            if (item == null) {
                item = XMaterial.ENDER_PEARL.parseItem();
            }
        } else {
            item = XMaterial.ENDER_PEARL.parseItem();
        }
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack buildPageInfoItem(int totalWarps) {
        XMaterial xMat = XMaterial.matchXMaterial(pageInfoMaterial).orElse(XMaterial.PAPER);
        ItemStack item = xMat.parseItem();
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = pageInfoName.replace("%count%", String.valueOf(totalWarps));
            meta.setDisplayName(ColorUtils.translateColors(name));
            List<String> lore = new ArrayList<>();
            for (String line : pageInfoLore) {
                lore.add(line.replace("%count%", String.valueOf(totalWarps)));
            }
            meta.setLore(translateLore(lore));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack buildSkullItem(String materialName, String skullTexture, String skullPlayer) {
        if (!isSkullMaterial(materialName)) return null;

        ItemStack skull = XMaterial.PLAYER_HEAD.parseItem();
        if (skull == null) return null;

        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        if (skullTexture != null && !skullTexture.isEmpty()) {
            applySkullTexture(meta, skullTexture);
        } else if (skullPlayer != null && !skullPlayer.isEmpty()) {
            applySkullPlayer(meta, skullPlayer);
        }

        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * Apply a base64 skull texture. Tries multiple strategies for cross-version support.
     */
    private void applySkullTexture(SkullMeta meta, String base64Texture) {
        if (base64Texture == null || base64Texture.isEmpty()) return;

        // Strategy 1: Paper/Spigot PlayerProfile API (most reliable when available)
        if (setSkullViaPlayerProfileAPI(meta, base64Texture)) return;

        // Strategy 2: GameProfile field injection (Spigot 1.8.8 – 1.20.4)
        setSkullViaGameProfile(meta, base64Texture);
    }

    /**
     * Try setting texture via Paper/Bukkit PlayerProfile API.
     * Handles multiple Paper versions (destroystokyo vs io.papermc namespaces).
     */
    private boolean setSkullViaPlayerProfileAPI(SkullMeta meta, String base64Texture) {
        String[] profilePropertyClasses = {
                "io.papermc.paper.profile.ProfileProperty",
                "com.destroystokyo.paper.profile.ProfileProperty"
        };

        for (String propClassName : profilePropertyClasses) {
            try {
                Class<?> propClass = Class.forName(propClassName);
                Object property = buildProfileProperty(propClass, base64Texture);
                if (property == null) continue;

                Object profile = Bukkit.class.getMethod("createProfile", UUID.class, String.class)
                        .invoke(null, UUID.randomUUID(), "CustomHead");
                Class<?> profileClass = profile.getClass();

                profileClass.getMethod("setProperty", propClass).invoke(profile, property);
                meta.getClass().getMethod("setPlayerProfile", profileClass).invoke(meta, profile);
                return true;
            } catch (ClassNotFoundException ignored) {
                continue;
            } catch (Exception ignored) {
                continue;
            }
        }
        return false;
    }

    /**
     * Create a ProfileProperty instance via reflection, handling different constructor
     * signatures: (String,String), (String,String,String), (String,String,Signature).
     */
    private Object buildProfileProperty(Class<?> propClass, String base64Texture) {
        for (java.lang.reflect.Constructor<?> ctor : propClass.getDeclaredConstructors()) {
            Class<?>[] pt = ctor.getParameterTypes();
            if (pt.length == 2 && pt[0] == String.class && pt[1] == String.class) {
                try {
                    ctor.setAccessible(true);
                    return ctor.newInstance("textures", base64Texture);
                } catch (Exception ignored) {
                }
            } else if (pt.length == 3 && pt[0] == String.class && pt[1] == String.class) {
                // Third param can be String (old) or PropertySignature (new) — pass null either way
                try {
                    ctor.setAccessible(true);
                    return ctor.newInstance("textures", base64Texture, (Object) null);
                } catch (Exception e1) {
                    try {
                        ctor.setAccessible(true);
                        return ctor.newInstance("textures", base64Texture, "");
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }

    private void applySkullPlayer(SkullMeta meta, String playerName) {
        try {
            Object profile = Bukkit.class.getMethod("createProfile", UUID.class, String.class)
                    .invoke(null, (UUID) null, playerName);
            meta.getClass().getMethod("setPlayerProfile", profile.getClass()).invoke(meta, profile);
        } catch (Exception e) {
            meta.setOwner(playerName);
        }
    }

    /**
     * Apply a base64 skull texture by directly setting the GameProfile field on CraftMetaSkull.
     * Uses constructor scanning to handle different authlib Property signatures across versions.
     */
    private void setSkullViaGameProfile(SkullMeta meta, String base64Texture) {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object gameProfile = gameProfileClass.getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), "CustomHead");

            // Create Property — scan constructors for (String,String) or (String,String,?)
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object property = buildProfileProperty(propertyClass, base64Texture);
            if (property == null) return;

            // Add property to GameProfile's property map (PropertyMap extends Multimap)
            Object properties = gameProfileClass.getMethod("getProperties").invoke(gameProfile);
            boolean added = false;
            for (java.lang.reflect.Method m : properties.getClass().getMethods()) {
                if (m.getName().equals("put") && m.getParameterCount() == 2) {
                    try {
                        m.invoke(properties, "textures", property);
                        added = true;
                    } catch (Exception ignored) {
                    }
                    break;
                }
            }
            if (!added) {
                try {
                    ((java.util.Map<Object, Object>) properties).put("textures",
                            java.util.Collections.singletonList(property));
                } catch (Exception ignored) {
                }
            }

            // Set the GameProfile onto the SkullMeta's internal 'profile' field
            java.lang.reflect.Field profileField = null;
            for (Class<?> clazz = meta.getClass(); clazz != null && profileField == null; clazz = clazz.getSuperclass()) {
                try {
                    profileField = clazz.getDeclaredField("profile");
                } catch (NoSuchFieldException ignored) {
                }
            }
            if (profileField != null) {
                profileField.setAccessible(true);
                profileField.set(meta, gameProfile);
            }
        } catch (Exception ignored) {
            // Custom textures are non-critical — fail silently
        }
    }

    private boolean isSkullMaterial(String materialName) {
        if (materialName == null) return false;
        String upper = materialName.toUpperCase();
        return upper.equals("PLAYER_HEAD") || upper.equals("HEAD")
                || upper.equals("SKULL_ITEM") || upper.equals("SKULL");
    }

    private List<String> translateLore(List<String> lore) {
        if (lore == null) return new ArrayList<>();
        return lore.stream()
                .map(ColorUtils::translateColors)
                .collect(Collectors.toList());
    }

    public void reload() {
        loadConfig();
        openWarpSlots.clear();
    }

    public void cleanupPlayer(Player player) {
        openWarpSlots.remove(player.getUniqueId());
    }

    public String getWarpAtSlot(Player player, int slot) {
        Map<Integer, String> slots = openWarpSlots.get(player.getUniqueId());
        if (slots == null) return null;
        return slots.get(slot);
    }

    public boolean isCloseSlot(int slot) {
        return slot == closeSlot;
    }

    public boolean isPageInfoSlot(int slot) {
        return pageInfoEnabled && slot == pageInfoSlot;
    }

    public boolean isPrevPageSlot(int slot) {
        return slot == prevPageSlot;
    }

    public boolean isNextPageSlot(int slot) {
        return slot == nextPageSlot;
    }
}
