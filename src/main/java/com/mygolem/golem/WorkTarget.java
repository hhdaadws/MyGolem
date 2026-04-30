package com.mygolem.golem;

import org.bukkit.Location;

public record WorkTarget(Type type, Location location) {

    public enum Type {
        HARVEST,
        PLANT
    }
}
