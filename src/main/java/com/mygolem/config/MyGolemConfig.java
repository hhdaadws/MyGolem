package com.mygolem.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MyGolemConfig {

    private final JavaPlugin plugin;
    private String modelId;
    private String baseEntity;
    private int radius;
    private long intervalTicks;
    private double actionDistance;
    private int verticalScanRadius;
    private int maxGolemsPerPlayer;
    private int maxGolemsGlobal;
    private int maxLoadedChunksPerGolem;
    private Path sqliteFile;
    private boolean eventProtectionEnabled;
    private List<String> seedPriority;
    private Map<String, Set<Integer>> harvestablePoints;
    private String prefix;

    public MyGolemConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        modelId = plugin.getConfig().getString("model.model-id", "farm_golem");
        baseEntity = plugin.getConfig().getString("model.base-entity", "ALLAY");
        radius = Math.max(1, plugin.getConfig().getInt("work.radius", 6));
        intervalTicks = Math.max(5L, plugin.getConfig().getLong("work.interval-ticks", 40L));
        actionDistance = Math.max(1.0D, plugin.getConfig().getDouble("work.action-distance", 2.5D));
        verticalScanRadius = Math.max(0, plugin.getConfig().getInt("work.vertical-scan-radius", 2));
        maxGolemsPerPlayer = Math.max(1, plugin.getConfig().getInt("limits.max-golems-per-player", 3));
        maxGolemsGlobal = Math.max(1, plugin.getConfig().getInt("limits.max-golems-global", 120));
        maxLoadedChunksPerGolem = Math.max(1, plugin.getConfig().getInt("limits.max-loaded-chunks-per-golem", 9));
        sqliteFile = plugin.getDataFolder().toPath().resolve(plugin.getConfig().getString("storage.sqlite-file", "mygolem.db"));
        eventProtectionEnabled = plugin.getConfig().getBoolean("protection.event-checks.enabled", true);
        seedPriority = plugin.getConfig().getStringList("crops.seed-priority").stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        harvestablePoints = loadHarvestablePoints();
        prefix = color(plugin.getConfig().getString("messages.prefix", "&6[MyGolem] &e"));
    }

    public boolean isHarvestable(String cropId, int point, int maxPoints) {
        Set<Integer> configured = harvestablePoints.get(normalize(cropId));
        if (configured != null && !configured.isEmpty()) {
            return configured.contains(point);
        }
        return point >= Math.max(0, maxPoints - 1);
    }

    private Map<String, Set<Integer>> loadHarvestablePoints() {
        Map<String, Set<Integer>> points = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("crops.harvestable-points");
        if (section == null) {
            return points;
        }
        for (String cropId : section.getKeys(false)) {
            points.put(normalize(cropId), new HashSet<>(section.getIntegerList(cropId)));
        }
        return points;
    }

    public Material controllerMaterial() {
        return Material.BLAZE_ROD;
    }

    public String modelId() {
        return modelId;
    }

    public String baseEntity() {
        return baseEntity;
    }

    public int radius() {
        return radius;
    }

    public long intervalTicks() {
        return intervalTicks;
    }

    public double actionDistance() {
        return actionDistance;
    }

    public int verticalScanRadius() {
        return verticalScanRadius;
    }

    public int maxGolemsPerPlayer() {
        return maxGolemsPerPlayer;
    }

    public int maxGolemsGlobal() {
        return maxGolemsGlobal;
    }

    public int maxLoadedChunksPerGolem() {
        return maxLoadedChunksPerGolem;
    }

    public Path sqliteFile() {
        return sqliteFile;
    }

    public boolean eventProtectionEnabled() {
        return eventProtectionEnabled;
    }

    public List<String> seedPriority() {
        return seedPriority;
    }

    public String prefix() {
        return prefix;
    }

    public String message(String text) {
        return prefix + color(text);
    }

    public static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
