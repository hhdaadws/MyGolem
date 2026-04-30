package com.mygolem.golem;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MenuHolder implements InventoryHolder {

    private final UUID golemId;
    private Inventory inventory;

    public MenuHolder(UUID golemId) {
        this.golemId = golemId;
    }

    public UUID golemId() {
        return golemId;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
