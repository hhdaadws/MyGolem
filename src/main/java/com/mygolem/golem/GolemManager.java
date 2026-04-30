package com.mygolem.golem;

import com.mygolem.chunk.ChunkTicketManager;
import com.mygolem.config.MyGolemConfig;
import com.mygolem.customcrops.CustomCropsFacade;
import com.mygolem.model.GolemRecord;
import com.mygolem.model.StoredLocation;
import com.mygolem.modelengine.ModelEngineAdapter;
import com.mygolem.protection.ProtectionService;
import com.mygolem.storage.BackpackInventory;
import com.mygolem.storage.BackpackSnapshot;
import com.mygolem.storage.BukkitItemStackCodec;
import com.mygolem.storage.CompositeStorageAdapter;
import com.mygolem.storage.GolemRepository;
import com.mygolem.storage.InventoryStorageAdapter;
import com.mygolem.storage.ItemStackCodec;
import com.mygolem.storage.StorageAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GolemManager {

    private final Plugin plugin;
    private final MyGolemConfig config;
    private final GolemRepository repository;
    private final CustomCropsFacade customCrops;
    private final ModelEngineAdapter modelEngine;
    private final ChunkTicketManager chunkTickets;
    private final ProtectionService protection;
    private final ItemStackCodec itemCodec = new BukkitItemStackCodec();
    private final NamespacedKey golemKey;
    private final Map<UUID, GolemRecord> records = new ConcurrentHashMap<>();
    private final Map<UUID, WorkSession> sessions = new ConcurrentHashMap<>();

    public GolemManager(
            Plugin plugin,
            MyGolemConfig config,
            GolemRepository repository,
            CustomCropsFacade customCrops,
            ModelEngineAdapter modelEngine,
            ChunkTicketManager chunkTickets,
            ProtectionService protection
    ) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.customCrops = customCrops;
        this.modelEngine = modelEngine;
        this.chunkTickets = chunkTickets;
        this.protection = protection;
        this.golemKey = new NamespacedKey(plugin, "golem_id");
    }

    public void load() throws SQLException {
        for (GolemRecord record : repository.loadAll()) {
            records.put(record.id(), record);
            if (GolemRecallPolicy.shouldSpawnOnLoad(record)) {
                ensureEntity(record);
            }
            if (record.active()) {
                start(record.id(), null);
            }
        }
    }

    public Collection<GolemRecord> all() {
        return records.values();
    }

    public Optional<GolemRecord> get(UUID id) {
        return Optional.ofNullable(records.get(id));
    }

    public UUID golemId(Entity entity) {
        if (entity == null) {
            return null;
        }
        String raw = entity.getPersistentDataContainer().get(golemKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public GolemRecord create(Player owner, Location location) {
        long owned = records.values().stream().filter(record -> record.owner().equals(owner.getUniqueId())).count();
        if (owned >= config.maxGolemsPerPlayer() || records.size() >= config.maxGolemsGlobal()) {
            owner.sendMessage(config.message("傀儡数量已达到上限。"));
            return null;
        }
        UUID id = UUID.randomUUID();
        StoredLocation stored = StoredLocation.from(location);
        GolemRecord record = new GolemRecord(id, owner.getUniqueId(), null, stored, stored, null, false, BackpackSnapshot.empty());
        Entity entity = spawnEntity(record);
        if (entity != null) {
            record = record.withEntityUuid(entity.getUniqueId());
        }
        save(record);
        records.put(id, record);
        return record;
    }

    public boolean start(UUID id, Player actor) {
        GolemRecord record = records.get(id);
        if (record == null) {
            return false;
        }
        if (!customCrops.isAvailable()) {
            if (actor != null) {
                actor.sendMessage(config.message("CustomCrops 不可用，无法启动傀儡。"));
            }
            save(record.withActive(false));
            return false;
        }
        stop(id, false);
        if (!chunkTickets.acquire(record, config.radius(), config.maxLoadedChunksPerGolem())) {
            if (actor != null) {
                actor.sendMessage(config.message("工作区需要加载的区块超过限制，已拒绝启动。"));
            }
            save(record.withActive(false));
            return false;
        }
        try {
            GolemRecord active = record.withActive(true);
            save(active);
            WorkSession session = new WorkSession(plugin, config, this, customCrops, protection, id);
            sessions.put(id, session);
            session.runTaskTimer(plugin, 1L, config.intervalTicks());
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Failed to start golem " + id + ": " + exception.getMessage());
            WorkSession failed = sessions.remove(id);
            if (failed != null) {
                failed.cancel();
            }
            chunkTickets.release(id, record.center().world());
            save(record.withActive(false));
            return false;
        }
    }

    public void stop(UUID id, boolean persist) {
        WorkSession session = sessions.remove(id);
        if (session != null) {
            session.cancel();
        }
        GolemRecord record = records.get(id);
        if (record != null) {
            chunkTickets.release(id, record.center().world());
            if (persist && record.active()) {
                save(record.withActive(false));
            }
        }
    }

    public void remove(UUID id) {
        GolemRecord record = records.get(id);
        if (record == null) {
            return;
        }
        try {
            repository.delete(id);
        } catch (SQLException exception) {
            plugin.getLogger().warning("Failed to delete golem " + id + ": " + exception.getMessage());
            return;
        }
        stop(id, false);
        records.remove(id);
        entity(record).ifPresent(entity -> {
            modelEngine.remove(entity);
            entity.remove();
        });
    }

    public void recall(UUID id) {
        stop(id, false);
        GolemRecord record = records.get(id);
        if (record == null) {
            return;
        }
        entity(record).ifPresent(entity -> {
            modelEngine.remove(entity);
            entity.remove();
        });
        save(GolemRecallPolicy.recall(record));
    }

    public GolemRecord summonExisting(UUID id, Location location) {
        GolemRecord record = records.get(id);
        StoredLocation stored = StoredLocation.from(location);
        if (record == null || stored == null) {
            return null;
        }
        Optional<Entity> existing = entity(record);
        if (existing.isPresent()) {
            return record;
        }
        GolemRecord spawning = record.withLocation(stored);
        Entity entity = spawnEntity(spawning);
        if (entity == null) {
            return null;
        }
        GolemRecord respawned = GolemRecallPolicy.respawn(record, stored, entity.getUniqueId());
        save(respawned);
        return respawned;
    }

    public void shutdown() {
        for (UUID id : Set.copyOf(sessions.keySet())) {
            stop(id, false);
        }
        for (GolemRecord record : records.values()) {
            save(record);
        }
    }

    public void setCenter(UUID id, Location center) {
        GolemRecord record = records.get(id);
        StoredLocation storedCenter = StoredLocation.from(center);
        if (record != null && storedCenter != null) {
            GolemRecord updated = GolemIdleReturnPolicy.withCenterAndIdleLocation(record, storedCenter);
            save(updated);
            Location anchor = idleAnchor(updated);
            if (anchor != null) {
                Entity entity = ensureEntity(updated);
                if (entity != null) {
                    teleportAndStop(entity, anchor);
                }
            }
        }
    }

    public void bindChest(UUID id, Location chest) {
        GolemRecord record = records.get(id);
        if (record != null) {
            save(record.withChest(StoredLocation.from(chest)));
        }
    }

    public void saveBackpack(UUID id, Inventory inventory) {
        GolemRecord record = records.get(id);
        if (record != null) {
            save(record.withBackpack(BackpackInventory.snapshot(inventory, itemCodec)));
        }
    }

    public Inventory createBackpackInventory(UUID id) {
        GolemRecord record = records.get(id);
        if (record == null) {
            return null;
        }
        BackpackHolder holder = new BackpackHolder(id);
        Inventory inventory = Bukkit.createInventory(holder, BackpackSnapshot.SIZE, "§0傀儡背包");
        holder.inventory(inventory);
        String[] slots = record.backpack().slots();
        for (int index = 0; index < slots.length; index++) {
            inventory.setItem(index, itemCodec.decode(slots[index]));
        }
        return inventory;
    }

    public Inventory createMenu(UUID id) {
        GolemRecord record = records.get(id);
        if (record == null) {
            return null;
        }
        MenuHolder holder = new MenuHolder(id);
        Inventory inventory = Bukkit.createInventory(holder, 9, "§0傀儡管理");
        holder.inventory(inventory);
        inventory.setItem(0, button(Material.LIME_DYE, record.active() ? "§c停止工作" : "§a启动工作"));
        inventory.setItem(2, button(Material.CHEST, "§6打开背包"));
        inventory.setItem(4, button(Material.COMPASS, "§e设置当前位置为中心"));
        inventory.setItem(6, button(Material.BARRIER, "§c解绑箱子"));
        inventory.setItem(8, button(Material.ENDER_PEARL, "§e收回傀儡"));
        return inventory;
    }

    public Optional<StorageAdapter> resolveStorage(GolemRecord record) {
        List<StorageAdapter> storages = new ArrayList<>();
        resolveBackpackStorage(record).ifPresent(storages::add);
        if (record.chest() != null) {
            Optional<InventoryStorageAdapter> chest = resolveChestStorage(record);
            if (chest.isEmpty()) {
                return Optional.empty();
            }
            storages.add(chest.get());
        }
        return Optional.of(new CompositeStorageAdapter(storages));
    }

    public Optional<InventoryStorageAdapter> resolveBackpackStorage(GolemRecord record) {
        Inventory backpack = createBackpackInventory(record.id());
        if (backpack == null) {
            return Optional.empty();
        }
        return Optional.of(new InventoryStorageAdapter(backpack, BackpackSnapshot.SIZE, () -> saveBackpack(record.id(), backpack)));
    }

    public Optional<InventoryStorageAdapter> resolveChestStorage(GolemRecord record) {
        if (record.chest() == null) {
            return Optional.empty();
        }
        Optional<Container> boundChest = container(record);
        if (boundChest.isEmpty()) {
            return Optional.empty();
        }
        Container container = boundChest.get();
        return Optional.of(new InventoryStorageAdapter(
                container.getInventory(),
                container.getInventory().getSize(),
                () -> container.update(true)
        ));
    }

    public Optional<Entity> entity(GolemRecord record) {
        if (record.entityUuid() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(Bukkit.getEntity(record.entityUuid()));
    }

    public boolean hasLiveEntity(UUID id) {
        GolemRecord record = records.get(id);
        return record != null && entity(record).isPresent();
    }

    public Entity ensureEntity(GolemRecord record) {
        return entity(record).orElseGet(() -> {
            Entity entity = spawnEntity(record);
            if (entity != null) {
                save(record.withEntityUuid(entity.getUniqueId()));
            }
            return entity;
        });
    }

    public void moveToward(GolemRecord record, Location target) {
        Entity entity = ensureEntity(record);
        if (entity != null) {
            moveEntityToward(entity, target);
        }
    }

    public double distanceToEntity(GolemRecord record, Location target) {
        return entity(record)
                .filter(entity -> entity.getWorld().equals(target.getWorld()))
                .map(entity -> entity.getLocation().distance(target))
                .orElse(Double.MAX_VALUE);
    }

    public void returnToIdle(GolemRecord record) {
        Location anchor = idleAnchor(record);
        if (anchor == null) {
            return;
        }
        Entity entity = ensureEntity(record);
        if (entity == null) {
            return;
        }
        Location current = entity.getLocation();
        double distance = entity.getWorld().equals(anchor.getWorld()) ? current.distance(anchor) : Double.MAX_VALUE;
        GolemIdleReturnPolicy.Action action = GolemIdleReturnPolicy.action(
                current.getWorld().getName(),
                anchor.getWorld().getName(),
                distance,
                config.actionDistance(),
                config.radius()
        );
        if (action == GolemIdleReturnPolicy.Action.MOVE) {
            moveEntityToward(entity, anchor);
            return;
        }
        teleportAndStop(entity, anchor);
    }

    public void save(GolemRecord record) {
        records.put(record.id(), record);
        try {
            repository.save(record);
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to save golem " + record.id() + ": " + exception.getMessage());
        }
    }

    private Optional<Container> container(GolemRecord record) {
        StoredLocation chest = record.chest();
        if (chest == null) {
            return Optional.empty();
        }
        Location location = chest.toBukkit();
        if (location == null || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return Optional.empty();
        }
        if (location.getBlock().getState() instanceof Container container) {
            return Optional.of(container);
        }
        return Optional.empty();
    }

    Location idleAnchor(GolemRecord record) {
        StoredLocation idle = GolemIdleReturnPolicy.idleStoredLocation(record.center());
        return idle == null ? null : idle.toBukkit();
    }

    private void teleportAndStop(Entity entity, Location anchor) {
        entity.teleport(anchor);
        stopMovement(entity);
    }

    private void moveEntityToward(Entity entity, Location target) {
        if (entity instanceof Mob mob) {
            mob.setAI(true);
            mob.getPathfinder().moveTo(target);
        } else {
            entity.teleport(target);
        }
    }

    private void stopMovement(Entity entity) {
        if (entity instanceof Mob mob) {
            mob.getPathfinder().stopPathfinding();
            mob.setAI(false);
        }
        entity.setVelocity(new Vector(0, 0, 0));
    }

    private Entity spawnEntity(GolemRecord record) {
        Location location = record.location().toBukkit();
        if (location == null) {
            return null;
        }
        World world = location.getWorld();
        EntityType type;
        try {
            type = EntityType.valueOf(config.baseEntity().toUpperCase());
        } catch (IllegalArgumentException exception) {
            type = EntityType.ALLAY;
        }
        Entity entity = world.spawnEntity(location, type);
        entity.getPersistentDataContainer().set(golemKey, PersistentDataType.STRING, record.id().toString());
        entity.setPersistent(true);
        entity.setCustomName("§6农场傀儡");
        entity.setCustomNameVisible(true);
        if (entity instanceof LivingEntity living) {
            living.setSilent(true);
            living.setInvulnerable(true);
            living.setRemoveWhenFarAway(false);
        }
        boolean modeled = modelEngine.apply(entity, config.modelId());
        if (modeled && entity instanceof LivingEntity living) {
            living.setInvisible(true);
        }
        return entity;
    }

    private ItemStack button(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public Comparator<WorkTarget> byDistance(GolemRecord record) {
        return Comparator.comparingDouble(target -> distanceToEntity(record, target.location()));
    }
}
