package com.mygolem.customcrops;

import org.bukkit.Location;

public record CropSnapshot(Location location, String cropId, int point, int maxPoints) {
}
