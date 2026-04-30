# MyGolem

Independent Paper plugin for ModelEngine farm golems that harvest and replant CustomCrops.

## Usage

1. Put `MyGolem-1.0.0.jar` in the Paper `plugins` folder.
2. Start the server once so `plugins/MyGolem/config.yml` and `mygolem.db` are created.
3. Give a controller:
   ```
   /mygolem give <player>
   ```
4. Hold the controller:
   - Right-click a block with no selected golem to summon one.
   - Right-click the golem to select it and open management.
   - Shift-right-click a block with a selected golem to set the work center.
   - Right-click a chest or barrel with a selected golem to bind storage.
5. Open the golem GUI to start or stop work, open the 1x9 backpack, unlink storage, or recall it.

## Runtime Notes

- Requires CustomCrops.
- Supports ModelEngine R4 when installed; without ModelEngine the base entity remains visible.
- Golem data and backpack contents are saved in SQLite with WAL and synchronous FULL.
- GUI recall removes only the live entity. The selected controller can summon the same golem again without clearing its backpack.
- `/mygolem remove <id>` is the admin-only permanent delete path.
- Work chunk tickets are only held while the golem is active and are released when it stops.

## Development

Implementation details and long-term maintenance notes are in `docs/DEVELOPMENT.md`.
