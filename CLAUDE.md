# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

中文回答。

## 项目概述

MyGolem 是一个独立的 Paper 1.21 插件，让 ModelEngine 装饰过的"农场傀儡"实体自动收割并补种 CustomCrops 作物。傀儡由玩家手持烈焰棒控制器召唤、选中、绑定容器；运行期数据持久化在插件目录下的 SQLite 文件。

实现细节、运行时假设、流程边界与"未来工作清单"维护在 `docs/DEVELOPMENT.md`，**修改插件时必须同步更新该文档**（明确写在文档第 26 行的维护规则）。

## 构建与测试命令

项目根目录是 Gradle 项目。Gradle wrapper 仅提供 `gradlew.bat`（Windows）；在 Linux/WSL 下用本机 Gradle 9.2.1 运行同样的任务。`--no-daemon` 是 DEVELOPMENT.md 给出的标准约束。

```bash
# 完整构建（含 shadowJar，输出 build/libs/MyGolem-1.0.0.jar）
./gradlew.bat build --no-daemon          # Windows
gradle build --no-daemon                 # Linux（系统 gradle）

# 仅运行测试
gradle test --no-daemon

# 单个测试类 / 单个方法
gradle test --tests com.mygolem.storage.BackpackSnapshotTest --no-daemon
gradle test --tests "com.mygolem.golem.WorkStoragePolicyTest.harvestUsesBackpackWhileSpaceAvailable" --no-daemon
```

### 关键依赖路径

构建要求以下相对路径与本地库可用，否则编译失败：

- `../Custom-Crops/target/CustomCrops-3.6.50.jar`（同级 `Custom-Crops` 项目编译产物，`compileOnly`）
- 本地 Maven 缓存中的 `com.ticxo.modelengine:ModelEngine:R4.0.6`（`compileOnly`，从 `https://mvn.lumine.io` / `https://repo.momirealms.net` 解析）
- `org.xerial:sqlite-jdbc:3.46.1.0` 通过 shadow 打入插件 jar（`implementation`）

Java 语言级别：toolchain 21，但字节码 release = 17（`build.gradle:11-18`）。

## 部署约定

- 部署目标（DEVELOPMENT.md 记载）：`E:\我的世界开发\26.1.2\plugins\MyGolem-1.0.0.jar`。每次只复制重建后的 jar，不要复制源码或构建中间产物。
- 启动一次后才会生成 `plugins/MyGolem/config.yml` 与 `plugins/MyGolem/mygolem.db`。这三个文件以及 `mygolem.db-wal` / `mygolem.db-shm` 都不要进仓库。
- `.gitignore` 已经排除 `.gradle/`、`build/`、`plugins/`、`mygolem.db*`；新增工具文件前先检查是否需要补充。

## 架构要点

`MyGolemPlugin#onEnable` 把所有服务一次性装配（见 `MyGolemPlugin.java:26-61`），关闭时通过 `GolemManager.shutdown()` 释放 chunk ticket 并保存所有记录。模块按职责分包：

- **`golem/` — 核心运行时**：`GolemManager` 是傀儡注册表；`WorkSession` 是单个傀儡的工作循环（每 `work.interval-ticks` 跑一次）；`WorkStoragePolicy` 决定要不要先卸货；`WorkTarget` 描述当前 tick 选中的目标。`MenuHolder` / `BackpackHolder` 仅用于安全识别插件 GUI。
- **`storage/` — SQLite 持久化 + 物品序列化**：`GolemRepository` 维护单表 `golems`；`BackpackSnapshot` 始终是 9 槽位，序列化用 `v` 前缀区分 null 与真实空字符串；`InventoryStacks` 模拟真实 Bukkit 容量/合并语义；`CompositeStorageAdapter` 保留为通用组合存储但农场流程已不再用它把收割物直接送进箱子。
- **`customcrops/` — CustomCrops 隔离层**：所有 CustomCrops API 只通过 `BukkitCustomCropsFacade` 调用；收割期间 `GolemDropRouter` 截断 CustomCrops 的 `DropItemActionEvent` / `QualityCropActionEvent`，把掉落改路由到傀儡背包。
- **`controller/` + `listener/`**：控制器物品 PDC 里存 `selected_golem`，实体 PDC 里存 `golem_id`。所有右键交互在 `ControllerListener` / `GolemEntityListener` 里分流；GUI 关闭时由 `MenuListener` 触发背包保存。
- **`modelengine/`**：可选依赖。`ModelEngineAdapter` 在 ModelEngine 缺失时静默退化为显示底层基础实体（默认 `ALLAY`）。
- **`chunk/`**：`ChunkTicketManager` 仅在傀儡 `start()` 时申请 plugin chunk ticket，`stop/recall/shutdown` 全部路径都必须释放。新增任何阻断启动或终止的代码路径都要复核 ticket 释放。
- **`protection/`**：`ProtectionService` 通过构造一个 `BlockBreakEvent` 让其他插件取消来判断作业权限；`ProtectionDecision` 是纯函数、有单元测试。

### 几条强约束（容易踩坑）

1. **不要 relocate `org.sqlite`**。SQLite JDBC 通过原始包名做 service 加载，shadow 配置故意没动它。
2. **背包是农场作业的唯一存储**。绑定箱子只在背包满 + 下一目标是收割时用作卸货终点；种植阶段不会跨容器取种子。
3. **召回 vs 删除**：GUI 召回只清 `active` 与 `entity_uuid`，记录、背包、中心、绑定箱子都保留；`/mygolem remove <id>` 才是永久删除（管理员命令，权限 `mygolem.admin`）。
4. **绑定箱子 chunk 必须已加载**。未加载时 `WorkSession` 会停掉傀儡而不是默默吞物品 —— 这是预期行为，不要"修"成自动加载远端 chunk。
5. **收割剩余物拒绝静默丢弃**：如果 `BukkitCustomCropsAPI.simulatePlayerBreakCrop` 返回的剩余物超过背包容量，会通知所有者并停止会话。改造此路径前先看 `WorkSession` 与 `StorageRoutingTest`。
6. **square scan**：作业范围按 `work.radius` 的方形扫，`limits.max-loaded-chunks-per-golem` 是启动期硬上限（超过直接拒启动并清 `active`）。

## 修改流程检查表（来自 DEVELOPMENT.md §"Safe Change Checklist"）

涉及存储、物品流转或 CustomCrops 作业行为的改动：

1. 先加 / 改对应单元测试；
2. `gradle test --no-daemon` 通过；
3. `gradle build --no-daemon` 通过；
4. 若改了 shadow / 依赖 / `plugin.yml`，检查 `build/libs/MyGolem-1.0.0.jar` 内容；
5. 同步更新 `docs/DEVELOPMENT.md`（包标记、流程、已知缺口都在那里）。
