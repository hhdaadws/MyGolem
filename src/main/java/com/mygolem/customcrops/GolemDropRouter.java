package com.mygolem.customcrops;

import com.mygolem.storage.StorageAdapter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class GolemDropRouter {

    private static final Map<String, Redirect> REDIRECTS = new ConcurrentHashMap<>();

    private GolemDropRouter() {
    }

    public static void register(Location location, StorageAdapter storage, Consumer<List<ItemStack>> overflow) {
        REDIRECTS.put(key(location), new Redirect(storage, overflow));
    }

    public static void unregister(Location location) {
        REDIRECTS.remove(key(location));
    }

    public static boolean redirect(Location location, ItemStack drop) {
        List<ItemStack> drops = new ArrayList<>();
        if (drop != null) {
            drops.add(drop);
        }
        return redirect(location, drops);
    }

    public static boolean redirect(Location location, List<ItemStack> drops) {
        Redirect redirect = REDIRECTS.get(key(location));
        if (redirect == null) {
            return false;
        }
        List<ItemStack> leftovers = redirect.storage.addItems(drops);
        if (!leftovers.isEmpty() && redirect.overflow != null) {
            redirect.overflow.accept(leftovers);
        }
        return true;
    }

    private static String key(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private record Redirect(StorageAdapter storage, Consumer<List<ItemStack>> overflow) {
    }
}
