package com.fabian.xsetwarps.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class WarpGUIHolder implements InventoryHolder {

    private final int page;

    public WarpGUIHolder(int page) {
        this.page = Math.max(page, 1);
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
