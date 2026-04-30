package com.mygolem.customcrops;

import com.mygolem.storage.StorageAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface CustomCropsFacade {

    boolean isAvailable();

    String customItemId(ItemStack itemStack);

    Optional<CropSnapshot> cropAt(Location location);

    Optional<PotSnapshot> potAt(Location location);

    boolean harvest(Player player, Location cropLocation, StorageAdapter storage, Consumer<List<ItemStack>> overflow);

    boolean plant(Player player, Location potLocation, StorageAdapter storage, List<String> seedPriority);
}
