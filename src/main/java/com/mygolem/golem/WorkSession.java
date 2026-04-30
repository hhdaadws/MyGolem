package com.mygolem.golem;

import com.mygolem.config.MyGolemConfig;
import com.mygolem.customcrops.CropSnapshot;
import com.mygolem.customcrops.CustomCropsFacade;
import com.mygolem.customcrops.PotSnapshot;
import com.mygolem.model.GolemRecord;
import com.mygolem.protection.ProtectionService;
import com.mygolem.storage.InventoryStorageAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WorkSession extends BukkitRunnable {

    private final Plugin plugin;
    private final MyGolemConfig config;
    private final GolemManager manager;
    private final CustomCropsFacade crops;
    private final ProtectionService protection;
    private final UUID golemId;
    private final List<ItemStack> pendingLeftovers = new ArrayList<>();

    public WorkSession(
            Plugin plugin,
            MyGolemConfig config,
            GolemManager manager,
            CustomCropsFacade crops,
            ProtectionService protection,
            UUID golemId
    ) {
        this.plugin = plugin;
        this.config = config;
        this.manager = manager;
        this.crops = crops;
        this.protection = protection;
        this.golemId = golemId;
    }

    @Override
    public void run() {
        GolemRecord record = manager.get(golemId).orElse(null);
        if (record == null) {
            manager.stop(golemId, false);
            return;
        }
        Player owner = Bukkit.getPlayer(record.owner());
        if (owner == null || !owner.isOnline()) {
            manager.stop(golemId, true);
            return;
        }
        if (!crops.isAvailable()) {
            owner.sendMessage(config.message("CustomCrops 不可用，傀儡已停止。"));
            manager.stop(golemId, true);
            return;
        }
        Optional<InventoryStorageAdapter> backpack = manager.resolveBackpackStorage(record);
        if (backpack.isEmpty()) {
            owner.sendMessage(config.message("找不到傀儡背包，傀儡已停止。"));
            manager.stop(golemId, true);
            return;
        }
        if (!pendingLeftovers.isEmpty()) {
            drainPendingLeftoversToChest(record, owner, backpack.get());
            return;
        }
        WorkStoragePolicy.Action action = WorkStoragePolicy.actionFor(
                backpack.get().emptySlotCount(),
                record.chest() != null
        );
        if (action == WorkStoragePolicy.Action.UNLOAD_BACKPACK_TO_CHEST) {
            unloadBackpack(record, owner, backpack.get());
            return;
        }
        Optional<WorkTarget> target = selectTarget(record, owner);
        if (target.isEmpty()) {
            manager.returnToIdle(record);
            return;
        }
        Location actionLocation = target.get().location();
        if (manager.distanceToEntity(record, actionLocation) > config.actionDistance()) {
            manager.moveToward(record, actionLocation.clone().add(0.5, 0, 0.5));
            return;
        }
        perform(owner, backpack.get(), target.get());
    }

    private Optional<WorkTarget> selectTarget(GolemRecord record, Player owner) {
        Location center = record.center().toBukkit();
        if (center == null) {
            return Optional.empty();
        }
        World world = center.getWorld();
        int radius = config.radius();
        int vertical = config.verticalScanRadius();
        List<WorkTarget> harvest = new ArrayList<>();
        List<WorkTarget> plant = new ArrayList<>();
        for (int x = center.getBlockX() - radius; x <= center.getBlockX() + radius; x++) {
            for (int z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; z++) {
                for (int y = center.getBlockY() - vertical; y <= center.getBlockY() + vertical; y++) {
                    Location location = new Location(world, x, y, z);
                    if (!protection.canWorkAt(owner, location)) {
                        continue;
                    }
                    Optional<CropSnapshot> crop = crops.cropAt(location);
                    if (crop.isPresent() && config.isHarvestable(crop.get().cropId(), crop.get().point(), crop.get().maxPoints())) {
                        harvest.add(new WorkTarget(WorkTarget.Type.HARVEST, location));
                        continue;
                    }
                    Optional<PotSnapshot> pot = crops.potAt(location);
                    if (pot.isPresent()) {
                        Location cropLocation = location.clone().add(0, 1, 0);
                        if (crops.cropAt(cropLocation).isEmpty() && cropLocation.getBlock().isPassable()) {
                            plant.add(new WorkTarget(WorkTarget.Type.PLANT, location));
                        }
                    }
                }
            }
        }
        harvest.sort(manager.byDistance(record));
        plant.sort(manager.byDistance(record));
        if (!harvest.isEmpty()) {
            return Optional.of(harvest.get(0));
        }
        if (!plant.isEmpty()) {
            return Optional.of(plant.get(0));
        }
        return Optional.empty();
    }

    private void perform(Player owner, InventoryStorageAdapter backpack, WorkTarget target) {
        if (target.type() == WorkTarget.Type.HARVEST) {
            boolean harvested = crops.harvest(owner, target.location(), backpack, leftovers -> overflow(owner, leftovers));
            if (harvested) {
                crops.plant(owner, target.location().clone().subtract(0, 1, 0), backpack, config.seedPriority());
            }
            return;
        }
        crops.plant(owner, target.location(), backpack, config.seedPriority());
    }

    private void unloadBackpack(GolemRecord record, Player owner, InventoryStorageAdapter backpack) {
        if (record.chest() == null) {
            owner.sendMessage(config.message("傀儡背包已满且未绑定箱子，傀儡已停止。"));
            manager.stop(golemId, true);
            return;
        }
        Optional<InventoryStorageAdapter> chest = manager.resolveChestStorage(record);
        if (chest.isEmpty()) {
            owner.sendMessage(config.message("找不到绑定箱子，傀儡已停止。"));
            manager.stop(golemId, true);
            return;
        }
        Location chestLocation = record.chest().toBukkit();
        if (chestLocation == null) {
            owner.sendMessage(config.message("找不到绑定箱子，傀儡已停止。"));
            manager.stop(golemId, true);
            return;
        }
        Location unloadLocation = chestLocation.clone().add(0.5, 0, 0.5);
        if (manager.distanceToEntity(record, unloadLocation) > config.actionDistance()) {
            manager.moveToward(record, unloadLocation);
            return;
        }
        List<ItemStack> leftovers = backpack.transferAllTo(chest.get());
        if (!leftovers.isEmpty()) {
            owner.sendMessage(config.message("绑定箱子已满，傀儡已停止。"));
            Bukkit.getScheduler().runTask(plugin, () -> manager.stop(golemId, true));
        }
    }

    private void drainPendingLeftoversToChest(GolemRecord record, Player owner, InventoryStorageAdapter backpack) {
        if (record.chest() == null) {
            stopWithOverflowMessage(owner);
            return;
        }
        Optional<InventoryStorageAdapter> chest = manager.resolveChestStorage(record);
        if (chest.isEmpty()) {
            stopWithOverflowMessage(owner);
            return;
        }
        Location chestLocation = record.chest().toBukkit();
        if (chestLocation == null) {
            stopWithOverflowMessage(owner);
            return;
        }
        Location unloadLocation = chestLocation.clone().add(0.5, 0, 0.5);
        if (manager.distanceToEntity(record, unloadLocation) > config.actionDistance()) {
            manager.moveToward(record, unloadLocation);
            return;
        }
        List<ItemStack> bufferOverflow = chest.get().addItems(pendingLeftovers);
        pendingLeftovers.clear();
        if (!bufferOverflow.isEmpty()) {
            stopWithOverflowMessage(owner);
            return;
        }
        List<ItemStack> backpackLeftovers = backpack.transferAllTo(chest.get());
        if (!backpackLeftovers.isEmpty()) {
            owner.sendMessage(config.message("绑定箱子已满，傀儡已停止。"));
            Bukkit.getScheduler().runTask(plugin, () -> manager.stop(golemId, true));
        }
    }

    private void overflow(Player owner, List<ItemStack> leftovers) {
        if (leftovers.isEmpty()) {
            return;
        }
        GolemRecord record = manager.get(golemId).orElse(null);
        if (record != null && record.chest() != null) {
            pendingLeftovers.addAll(leftovers);
            return;
        }
        stopWithOverflowMessage(owner);
    }

    private void stopWithOverflowMessage(Player owner) {
        pendingLeftovers.clear();
        owner.sendMessage(config.message("傀儡背包装不下本次收获，傀儡已停止。"));
        Bukkit.getScheduler().runTask(plugin, () -> manager.stop(golemId, true));
    }
}
