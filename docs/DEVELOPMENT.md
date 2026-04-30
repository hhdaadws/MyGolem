# MyGolem Development Notes

This document records the implementation details that matter for long-term maintenance. It is intentionally more technical than `README.md`.

## Build And Runtime Baseline

- Project root: `E:\mcotherplugins\MyGolem`
- Live deployment target: `E:\我的世界开发\26.1.2\plugins\MyGolem-1.0.0.jar`
- Build command:
  ```powershell
  .\gradlew.bat build --no-daemon
  ```
- Built jar: `build/libs/MyGolem-1.0.0.jar`
- Java compile target is Java 17 bytecode, using the Java 21 toolchain.
- The Gradle wrapper was copied from the local `Custom-Crops` project and uses Gradle `9.2.1`.
- `CustomCrops` is compiled from the local jar dependency:
  `../Custom-Crops/target/CustomCrops-3.6.50.jar`
- `ModelEngine` is compile-only via local Maven cache: `com.ticxo.modelengine:ModelEngine:R4.0.6`.
- SQLite is shaded into the jar through `org.xerial:sqlite-jdbc:3.46.1.0`; do not relocate `org.sqlite`, because JDBC driver loading depends on the original driver class/service metadata.

## Repository Maintenance Notes

- Keep the source repository focused on Gradle project inputs: `src/`, `gradle/`, wrapper files, build scripts, README, and docs.
- Do not commit generated Gradle state or build outputs such as `.gradle/`, `.gradle-sandbox/`, `.gradle-user/`, `build/`, compiled classes, test reports, or rebuilt jars.
- Do not commit live runtime data such as `plugins/`, `mygolem.db`, `mygolem.db-wal`, or `mygolem.db-shm`.
- When modifying the plugin, update this long-term details and maintenance document in the same change set so future work keeps the operational assumptions current.

## Package Map

- `com.mygolem.MyGolemPlugin`
  - Bukkit plugin entrypoint.
  - Loads config, opens SQLite, creates all services, registers listeners and `/mygolem`.
  - On disable, stops work sessions, saves records, releases tickets through session stop, and closes SQLite.

- `com.mygolem.config`
  - `MyGolemConfig` owns all runtime config values and message formatting.
  - Important config paths: `model.model-id`, `work.radius`, `work.interval-ticks`, `work.action-distance`, `limits.max-loaded-chunks-per-golem`, `storage.sqlite-file`, `crops.seed-priority`.

- `com.mygolem.controller`
  - `ControllerItem` creates and identifies the blaze-rod controller.
  - The selected golem id is stored directly in item PDC under `selected_golem`.

- `com.mygolem.golem`
- `GolemManager` is the central runtime registry and owns create/start/stop/recall/remove/save flows.
  - `WorkSession` is the repeating farm loop for one active golem.
  - `WorkTarget` describes the selected action target: `HARVEST` or `PLANT`.
  - `WorkStoragePolicy` decides whether a selected target can use the backpack now or must unload first.
  - `MenuHolder` and `BackpackHolder` identify plugin inventories safely.

- `com.mygolem.customcrops`
  - `BukkitCustomCropsFacade` isolates direct CustomCrops API calls.
  - `GolemDropRouter` temporarily redirects CustomCrops drop events into the golem backpack during simulated harvest.
  - `CropSnapshot` and `PotSnapshot` are small read models used by the work scanner.

- `com.mygolem.storage`
  - `GolemRepository` is the SQLite persistence layer.
  - `BackpackSnapshot` is the durable 9-slot serialized backpack model.
  - `BukkitItemStackCodec` converts Bukkit `ItemStack` values to Base64 and back.
  - `InventoryStorageAdapter` wraps Bukkit inventories behind a common storage interface.
  - `InventoryStacks` implements the real Bukkit `ItemStack` capacity and transfer rules used by backpack and chest adapters.
  - `CompositeStorageAdapter` remains available for generic storage composition, but the farm loop no longer uses it to send harvest drops directly to chests.

- `com.mygolem.chunk`
  - `ChunkAreaCalculator` calculates chunk coverage for a center/radius.
  - `ChunkTicketManager` owns plugin chunk tickets while golems are active.

- `com.mygolem.modelengine`
  - `ModelEngineAdapter` applies/removes ModelEngine R4 models if ModelEngine is installed.
  - If ModelEngine is missing or model application fails, the base entity remains visible.

- `com.mygolem.protection`
  - `ProtectionService` blocks non-owner management and uses Bukkit protection events for work permission checks.
  - `ProtectionDecision` is intentionally pure and unit-tested.

- `com.mygolem.listener`
  - `ControllerListener`: controller right-click behavior for summoning, selecting, setting center, and binding chests.
  - `GolemEntityListener`: right-click golem management and damage prevention.
  - `MenuListener`: GUI button behavior plus backpack save-on-close.
  - `DropRedirectListener`: CustomCrops drop-event interception.

- `com.mygolem.command`
  - `MyGolemCommand` implements `/mygolem give|reload|list|remove|debug`.

## Data Model And SQLite Safety

`GolemRepository` creates one table: `golems`.

Important columns:

- `id`: stable golem UUID, primary key.
- `owner`: owner player UUID.
- `entity_uuid`: current Bukkit entity UUID. This can change if the entity is missing and gets recreated.
- `world/x/y/z/yaw/pitch`: golem body location.
- `center_world/center_x/center_y/center_z/center_yaw/center_pitch`: work center.
- `chest_world/chest_x/chest_y/chest_z/chest_yaw/chest_pitch`: optional bound container location.
- `active`: whether the golem should resume work on plugin load.
- `backpack`: serialized `BackpackSnapshot`.
- `updated_at`: last write timestamp.

SQLite is configured with:

```sql
PRAGMA journal_mode=WAL;
PRAGMA synchronous=FULL;
PRAGMA foreign_keys=ON;
```

Durability rules:

- `GolemManager.save(...)` updates both in-memory state and SQLite.
- Backpack changes are saved on inventory close and also through storage adapter callbacks after automated item routing.
- Chest binding, center changes, active state changes, entity UUID repairs, and removal are persisted immediately.
- GUI recall saves the same record with `active=false` and `entity_uuid=NULL`; the backpack, center, chest binding, and owner stay intact.
- `/mygolem remove <id>` is the permanent delete path and removes the SQLite row.
- On plugin disable, `GolemManager.shutdown()` stops sessions and saves all records again.
- If CustomCrops is unavailable, a golem cannot start and its active flag is cleared.
- If the work area exceeds the configured chunk-ticket limit, startup is rejected and active is cleared.
- If a golem has a bound chest but the chest is missing/unloaded at storage resolution time, storage resolution fails and the golem stops instead of silently losing items.

## Backpack Serialization

`BackpackSnapshot` always requires exactly 9 slots.

The snapshot stores each slot as a semicolon-separated field:

- Empty/null slot: empty field.
- Non-null slot: field starts with `v`, followed by Base64 UTF-8 payload.

The `v` prefix is deliberate. It preserves a real empty string differently from a null slot in tests and avoids ambiguous serialization.

In runtime, Bukkit items are encoded by `BukkitItemStackCodec` using `BukkitObjectOutputStream` and decoded with `BukkitObjectInputStream`.

## Player Flow

Controller item behavior:

- `/mygolem give <player>` gives the controller.
- Right-click a normal block with no selected golem: summon a new golem at the clicked block plus one block up.
- Right-click a normal block with a selected recalled golem: summon the same golem again at the clicked block plus one block up.
- Right-click a golem: select it and open management GUI.
- Shift-right-click a block with a selected golem: set work center.
- Right-click a container with a selected golem: bind that container as secondary storage.
- Right-click normal block with a selected golem: open management GUI.

GUI slots:

- Slot `0`: start or stop work.
- Slot `2`: open 1x9 backpack.
- Slot `4`: set current player block as work center.
- Slot `6`: unlink chest.
- Slot `8`: recall the golem. This removes the live entity only; durable data and backpack contents remain saved.

Only the owner can manage a golem. Admin commands require `mygolem.admin`. `/mygolem remove <id>` remains the permanent delete command.

## Entity And ModelEngine Flow

`GolemManager.spawnEntity(...)`:

1. Reads `model.base-entity`; defaults to `ALLAY` if invalid.
2. Spawns the base entity at the stored location.
3. Writes golem id into entity PDC under `golem_id`.
4. Marks entity persistent, invulnerable, silent, custom-named, and non-despawning if it is a `LivingEntity`.
5. Calls `ModelEngineAdapter.apply(...)`.
6. If ModelEngine succeeds, the base living entity is made invisible.

`GolemManager.recall(...)`:

1. Stops any active work session and releases chunk tickets.
2. Removes the ModelEngine model and the live Bukkit entity.
3. Saves the record through `GolemRecallPolicy.recall(...)`, clearing only `active` and `entity_uuid`.

`GolemManager.summonExisting(...)`:

1. Requires an existing saved record.
2. Spawns a new live entity at the clicked block plus one block up.
3. Saves the new `entity_uuid` and body location through `GolemRecallPolicy.respawn(...)`.
4. Leaves backpack, center, chest binding, owner, and id unchanged.

On plugin load, records with `entity_uuid=NULL` are treated as recalled and are not spawned automatically. They return only when the selected controller summons them again.

`ModelEngineAdapter.apply(...)` uses:

```java
ModeledEntity modeledEntity = ModelEngineAPI.getOrCreateModeledEntity(entity);
ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
modeledEntity.addModel(activeModel, true);
```

Known limitation: model animations/states are not yet customized. The current implementation only attaches the configured model.

## Work Session Flow

`GolemManager.start(...)`:

1. Validates CustomCrops availability.
2. Acquires chunk tickets for the configured work radius.
3. Stops any existing session for the same golem.
4. Saves `active=true`.
5. Starts a `WorkSession` repeating every `work.interval-ticks`.

`WorkSession.run()`:

1. Requires the owner to be online. If offline, stops and persists inactive state.
2. Requires CustomCrops to remain available.
3. Resolves the golem backpack as the only farm-work storage.
4. Scans the configured center/radius and vertical range.
5. Skips locations denied by `ProtectionService`.
6. Builds harvest targets from mature CustomCrops crops.
7. Builds plant targets from empty pots with passable crop space above.
8. Harvest targets win over plant targets.
9. The nearest target to the golem entity is selected.
10. If the target is harvest and the backpack has no empty slot or mergeable stack space, the golem unloads before harvesting.
11. If too far from the selected farm target or bound chest, the golem moves there with Bukkit mob pathfinding.
12. If close enough, it performs harvest, plant, or unload.

Harvest behavior:

- `BukkitCustomCropsAPI.get().simulatePlayerBreakCrop(...)` is called as the owner.
- During the harvest, `GolemDropRouter` maps the crop location to the golem backpack only.
- `DropItemActionEvent` and `QualityCropActionEvent` are cancelled once their drops are routed to storage.
- Harvest drops are not routed directly into the bound chest.
- If a harvest unexpectedly returns leftovers from the backpack, the owner is notified and the session stops.
- After successful harvest, the golem immediately attempts to replant using seeds from the backpack only.

Plant behavior:

- Finds a seed matching `crops.seed-priority` first.
- Falls back to any seed whose CustomCrops crop whitelist accepts the target pot id.
- Uses CustomCrops `BuiltInItemMechanics.SEED.mechanic().interactAt(...)`.
- Removes one seed only after CustomCrops reports complete interaction and a crop appears.
- Bound chest seeds are not used remotely. To plant, the seed must already be in the golem backpack.

Unload behavior:

- The bound chest is used only when the backpack is full and the selected next target is harvest.
- If no chest is bound, the owner is notified and the golem stops before harvesting.
- If the bound chest is missing, not a container, or its chunk is not loaded, the owner is notified and the golem stops.
- The golem moves to the bound chest first. Only after reaching action distance does it transfer backpack contents into the chest.
- Chest transfer first merges similar stacks, then uses empty slots, and only returns true leftovers when no allowed slot can accept them.
- On successful unload, the backpack is cleared and saved, then the next work tick resumes normal farm scanning.
- If the chest cannot fit all backpack contents, items that did not fit stay in the backpack, the owner is notified, and the golem stops.

Overflow behavior:

- The code does not intentionally drop overflow items or teleport harvest drops into the chest.
- The expected path is: harvest into backpack, walk to chest when backpack is full, deposit backpack into chest.
- Leftovers after harvest or unload stop the session so items are not silently duplicated or lost.

## Chunk Loading

Chunk loading is active only while a golem is working.

`ChunkAreaCalculator.coveredChunks(centerX, centerZ, radius)` calculates all chunks touched by the square scan area. `ChunkTicketManager.acquire(...)` rejects startup if the calculated set is larger than `limits.max-loaded-chunks-per-golem`.

Tickets are added with:

```java
world.addPluginChunkTicket(chunk.x(), chunk.z(), plugin);
```

Tickets are removed on stop:

```java
world.removePluginChunkTicket(chunk.x(), chunk.z(), plugin);
```

Maintenance rule: every new code path that stops, removes, disables, or fails a golem startup must make sure tickets are released or never acquired.

## Protection Notes

Current protection is conservative but generic:

- Non-owner interaction is blocked.
- Golem entity damage is always cancelled.
- Work checks create a `BlockBreakEvent` for the owner at the target block.
- If another plugin cancels that event, MyGolem skips the target.

The current live server has `ResidenceCore`, but its jar is obfuscated under `com.mythmc.res...`, so this implementation avoids compile-time Residence API calls. A future dedicated Residence hook should be added only after verifying the exact public API for this installed jar.

## Tests

Current tests:

- `BackpackSnapshotTest`
  - Verifies 9-slot serialization/deserialization.
  - Verifies non-9-slot snapshots are rejected.

- `GolemRepositoryTest`
  - Saves a full golem record to SQLite.
  - Reopens the database and verifies owner, center, chest, active state, and backpack slots.

- `StorageRoutingTest`
  - Verifies backpack-first routing.
  - Verifies overflow goes into secondary storage and real leftovers are returned.

- `StackTransferPlanTest`
  - Verifies the capacity rules used by the storage transfer algorithm: empty slots, mergeable stacks, and real leftovers.

- `WorkStoragePolicyTest`
  - Verifies harvest uses the backpack while there is space.
  - Verifies full-backpack harvest selects unload before work.
  - Verifies planting still uses backpack storage only.

- `ChunkAreaCalculatorTest`
  - Verifies radius-to-chunk coverage.
  - Verifies limit rejection.

- `ProtectionDecisionTest`
  - Verifies strict check aggregation.

Verification command:

```powershell
.\gradlew.bat build --no-daemon
```

Last verified result during implementation:

- Build successful.
- Jar contained:
  - `plugin.yml`
  - `com/mygolem/MyGolemPlugin.class`
  - `org/sqlite/JDBC.class`

## Known Gaps And Next Work

- No live server startup test was run during implementation because port `25565` was not listening.
- No custom ModelEngine animation state is set yet.
- No explicit ResidenceCore hook exists yet; protection currently relies on Bukkit event cancellation.
- Work scanning is square-area based. If performance becomes an issue, optimize by caching known CustomCrops pot/crop locations or scanning incrementally.
- Golem pathfinding uses Bukkit `Mob#getPathfinder().moveTo(...)`. If a configured base entity is not a `Mob`, the fallback is teleporting to target.
- Bound chest storage requires the chest chunk to be loaded. This is intentional to avoid loading arbitrary distant storage silently.
- Watering support was present in the earlier MCPets reference, but this independent v1 implements harvest and planting only. Add watering as a separate `WorkTarget.Type.WATER` pass if needed.
- Admin sharing, upgrades, energy costs, cross-world logistics, and sell/economy hooks are intentionally out of scope for v1.

## Safe Change Checklist

Before changing storage, item routing, or CustomCrops work behavior:

1. Add or update a focused unit test first.
2. Run `.\gradlew.bat test --no-daemon`.
3. Run `.\gradlew.bat build --no-daemon`.
4. Inspect `build/libs/MyGolem-1.0.0.jar` if dependency shading or plugin metadata changed.
5. If deploying live, copy only the rebuilt jar to the target plugins folder.
6. On first runtime check, confirm `plugins/MyGolem/mygolem.db` is created and the server log has no enable-time stack trace.
