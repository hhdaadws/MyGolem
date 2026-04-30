package com.mygolem.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {

    public static StoredLocation from(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new StoredLocation(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public Location toBukkit() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    public int blockX() {
        return (int) Math.floor(x);
    }

    public int blockY() {
        return (int) Math.floor(y);
    }

    public int blockZ() {
        return (int) Math.floor(z);
    }
}
