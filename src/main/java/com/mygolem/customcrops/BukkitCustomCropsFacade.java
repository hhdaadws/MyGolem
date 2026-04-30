package com.mygolem.customcrops;

import com.mygolem.storage.StorageAdapter;
import com.mygolem.storage.StorageSlot;
import net.momirealms.customcrops.api.BukkitCustomCropsAPI;
import net.momirealms.customcrops.api.BukkitCustomCropsPlugin;
import net.momirealms.customcrops.api.CustomCropsAPI;
import net.momirealms.customcrops.api.core.BuiltInItemMechanics;
import net.momirealms.customcrops.api.core.ExistenceForm;
import net.momirealms.customcrops.api.core.InteractionResult;
import net.momirealms.customcrops.api.core.Registries;
import net.momirealms.customcrops.api.core.block.BreakReason;
import net.momirealms.customcrops.api.core.block.CropBlock;
import net.momirealms.customcrops.api.core.block.PotBlock;
import net.momirealms.customcrops.api.core.item.CustomCropsItem;
import net.momirealms.customcrops.api.core.mechanic.crop.CropConfig;
import net.momirealms.customcrops.api.core.mechanic.pot.PotConfig;
import net.momirealms.customcrops.api.core.world.CustomCropsBlockState;
import net.momirealms.customcrops.api.core.world.CustomCropsWorld;
import net.momirealms.customcrops.api.core.world.Pos3;
import net.momirealms.customcrops.api.core.wrapper.WrappedInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class BukkitCustomCropsFacade implements CustomCropsFacade {

    private final Plugin plugin;

    public BukkitCustomCropsFacade(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("CustomCrops") && BukkitCustomCropsAPI.get() != null;
    }

    @Override
    public String customItemId(ItemStack itemStack) {
        if (!isAvailable() || itemStack == null || itemStack.getType().isAir()) {
            return "";
        }
        String id = BukkitCustomCropsPlugin.getInstance().getItemManager().id(itemStack);
        return id == null ? "" : id.toLowerCase();
    }

    @Override
    public Optional<CropSnapshot> cropAt(Location location) {
        return stateAt(location).filter(state -> state.type() instanceof CropBlock).flatMap(state -> {
            CropBlock cropBlock = (CropBlock) state.type();
            CropConfig config = cropBlock.config(state);
            if (config == null) {
                return Optional.empty();
            }
            return Optional.of(new CropSnapshot(location, config.id(), cropBlock.point(state), config.maxPoints()));
        });
    }

    @Override
    public Optional<PotSnapshot> potAt(Location location) {
        return stateAt(location).filter(state -> state.type() instanceof PotBlock).flatMap(state -> {
            PotBlock potBlock = (PotBlock) state.type();
            PotConfig config = potBlock.config(state);
            if (config == null) {
                return Optional.empty();
            }
            return Optional.of(new PotSnapshot(location, config.id(), potBlock.water(state), config.storage()));
        });
    }

    @Override
    public boolean harvest(Player player, Location cropLocation, StorageAdapter storage, Consumer<List<ItemStack>> overflow) {
        if (!isAvailable() || cropAt(cropLocation).isEmpty()) {
            return false;
        }
        GolemDropRouter.register(cropLocation, storage, overflow);
        ItemStack savedMainHand = player.getInventory().getItemInMainHand().clone();
        try {
            try {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                BukkitCustomCropsAPI.get().simulatePlayerBreakCrop(player, EquipmentSlot.HAND, cropLocation, BreakReason.BREAK);
            } finally {
                player.getInventory().setItemInMainHand(savedMainHand);
            }
        } finally {
            GolemDropRouter.unregister(cropLocation);
        }
        return cropAt(cropLocation).isEmpty();
    }

    @Override
    public boolean plant(Player player, Location potLocation, StorageAdapter storage, List<String> seedPriority) {
        if (!isAvailable()) {
            return false;
        }
        CustomCropsWorld<?> world = world(potLocation).orElse(null);
        if (world == null) {
            return false;
        }
        String potItemId = BukkitCustomCropsPlugin.getInstance().getItemManager().blockID(potLocation);
        PotConfig potConfig = Registries.ITEM_TO_POT.get(potItemId);
        if (potConfig == null) {
            return false;
        }
        Location cropLocation = potLocation.clone().add(0, 1, 0);
        if (cropAt(cropLocation).isPresent() || cropLocation.getBlock().getType() != Material.AIR) {
            return false;
        }
        Optional<StorageSlot> slot = findSeed(storage, seedPriority, potConfig);
        if (slot.isEmpty()) {
            return false;
        }
        String seedId = customItemId(slot.get().itemStack());
        CropConfig cropConfig = Registries.SEED_TO_CROP.get(seedId);
        if (cropConfig == null || !cropConfig.potWhitelist().contains(potConfig.id())) {
            return false;
        }
        ItemStack oneSeed = slot.get().itemStack().clone();
        oneSeed.setAmount(1);
        CustomCropsItem seedItem = BuiltInItemMechanics.SEED.mechanic();
        WrappedInteractEvent event = new WrappedInteractEvent(
                ExistenceForm.BLOCK,
                player,
                world,
                potLocation.clone(),
                potItemId,
                oneSeed,
                seedId,
                EquipmentSlot.HAND,
                BlockFace.UP,
                new SimpleCancellable()
        );
        ItemStack savedMainHand = player.getInventory().getItemInMainHand().clone();
        InteractionResult result;
        try {
            player.getInventory().setItemInMainHand(oneSeed);
            result = seedItem.interactAt(event);
        } finally {
            player.getInventory().setItemInMainHand(savedMainHand);
        }
        if (result == InteractionResult.COMPLETE && cropAt(cropLocation).isPresent()) {
            storage.removeOne(slot.get());
            return true;
        }
        return false;
    }

    private Optional<StorageSlot> findSeed(StorageAdapter storage, List<String> seedPriority, PotConfig potConfig) {
        List<String> priority = seedPriority == null ? List.of() : seedPriority;
        for (String seedId : priority) {
            CropConfig crop = Registries.SEED_TO_CROP.get(seedId);
            if (crop == null || !crop.potWhitelist().contains(potConfig.id())) {
                continue;
            }
            Optional<StorageSlot> configured = storage.findFirstCustomItem(List.of(seedId), this::customItemId);
            if (configured.isPresent()) {
                return configured;
            }
        }
        return storage.findFirstMatching(item -> {
            CropConfig crop = Registries.SEED_TO_CROP.get(customItemId(item));
            return crop != null && crop.potWhitelist().contains(potConfig.id());
        });
    }

    private Optional<CustomCropsBlockState> stateAt(Location location) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        return world(location).flatMap(world -> world.getLoadedBlockState(Pos3.from(location)));
    }

    private Optional<CustomCropsWorld<?>> world(Location location) {
        CustomCropsAPI api = BukkitCustomCropsAPI.get();
        if (api == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(api.getCustomCropsWorld(location.getWorld()));
    }

    private static class SimpleCancellable implements Cancellable {
        private boolean cancelled;

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            cancelled = cancel;
        }
    }
}
