package com.fabian.xsetwarps.gui;

import com.fabian.xsetwarps.XSetWarps;
import com.fabian.xsetwarps.model.Warp;
import com.fabian.xsetwarps.utils.ColorUtils;

import com.cryptomorin.xseries.XMaterial;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

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

        fillerMaterial = config.getString("filler.material", "BLACK_STAINED_GLASS_PANE");
        fillerName = ColorUtils.translateColors(config.getString("filler.name", " "));

        closeSlot = config.getInt("buttons.close.slot", 31);
        closeMaterial = config.getString("buttons.close.material", "PLAYER_HEAD");
        closeTexture = config.getString("buttons.close.skull-texture", "");
        closeName = ColorUtils.translateColors(config.getString("buttons.close.name", "&fClose"));
        closeLore = translateLore(config.getStringList("buttons.close.lore"));

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
                    meta.setDisplayName(ColorUtils.translateColors("&e&l« Previous Page"));
                    meta.setLore(Collections.singletonList(
                            ColorUtils.translateColors("&7Page " + (currentPage - 1) + " of " + totalPages)));
                    prevItem.setItemMeta(meta);
                }
            }
            if (prevItem != null) {
                inv.setItem(27, prevItem);
            }
        }

        if (currentPage < totalPages) {
            ItemStack nextItem = XMaterial.ARROW.parseItem();
            if (nextItem != null) {
                ItemMeta meta = nextItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ColorUtils.translateColors("&e&lNext Page »"));
                    meta.setLore(Collections.singletonList(
                            ColorUtils.translateColors("&7Page " + (currentPage + 1) + " of " + totalPages)));
                    nextItem.setItemMeta(meta);
                }
            }
            if (nextItem != null) {
                inv.setItem(35, nextItem);
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
        lore.add(ColorUtils.translateColors("&7Click to teleport!"));

        ItemStack item = XMaterial.ENDER_PEARL.parseItem();
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
            PlayerProfile profile = Bukkit.createProfile(null, skullPlayer);
            meta.setPlayerProfile(profile);
        }

        skull.setItemMeta(meta);
        return skull;
    }

    private void applySkullTexture(SkullMeta meta, String base64Texture) {
        if (base64Texture == null || base64Texture.isEmpty()) return;

        try {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "CustomHead");
            profile.setProperty(new ProfileProperty("textures", base64Texture));
            meta.setPlayerProfile(profile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply skull texture: " + e.getClass().getSimpleName());
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
}
