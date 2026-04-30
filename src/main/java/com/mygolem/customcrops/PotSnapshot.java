package com.mygolem.customcrops;

import org.bukkit.Location;

public record PotSnapshot(Location location, String potId, int water, int maxWater) {

    public boolean needsWater() {
        return water < maxWater;
    }
}
