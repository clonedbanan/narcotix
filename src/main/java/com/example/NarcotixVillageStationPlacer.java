package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NarcotixVillageStationPlacer {
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final int VILLAGE_SECTION_RANGE = 3;
    private static final int CELL_SIZE = 160;
    private static final int SEARCH_RADIUS = 42;
    private static final int HALF_FOOTPRINT = 6;

    private static boolean registered = false;
    private static int tickCounter = 0;
    private static final Set<String> placedVillageCells = new HashSet<>();
    private static boolean loaded = false;

    private NarcotixVillageStationPlacer() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < CHECK_INTERVAL_TICKS) {
                return;
            }
            tickCounter = 0;
            tick(server);
        });

        NarcotixMod.LOGGER.info("Registered Narcotix village police station placer.");
    }

    private static void tick(MinecraftServer server) {
        loadPlacedCells(server);

        for (ServerLevel level : server.getAllLevels()) {
            List<ServerPlayer> players = level.players();
            for (ServerPlayer player : players) {
                BlockPos playerPos = player.blockPosition();

                if (!isCloseToVillage(level, playerPos, VILLAGE_SECTION_RANGE)) {
                    continue;
                }

                String key = makeVillageCellKey(level, playerPos);
                if (placedVillageCells.contains(key)) {
                    continue;
                }

                BlockPos placePos = findPlacementPos(level, playerPos);
                if (placePos == null) {
                    continue;
                }

                String templateId = chooseTemplate(level, playerPos);
                if (placeTemplate(server, templateId, placePos)) {
                    placedVillageCells.add(key);
                    savePlacedCells(server);
                    NarcotixMod.LOGGER.info("Placed Narcotix police station {} for village cell {} at {} {} {}.", templateId, key, placePos.getX(), placePos.getY(), placePos.getZ());
                }
            }
        }
    }

    private static boolean isCloseToVillage(ServerLevel level, BlockPos pos, int sectionRange) {
        try {
            Method method = level.getClass().getMethod("isCloseToVillage", BlockPos.class, int.class);
            Object result = method.invoke(level, pos, sectionRange);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String makeVillageCellKey(ServerLevel level, BlockPos pos) {
        String dimension = String.valueOf(level.dimension());
        int cellX = Math.floorDiv(pos.getX(), CELL_SIZE);
        int cellZ = Math.floorDiv(pos.getZ(), CELL_SIZE);
        return dimension + ":" + cellX + ":" + cellZ;
    }

    private static String chooseTemplate(ServerLevel level, BlockPos pos) {
        String biomeText = String.valueOf(level.getBiome(pos)).toLowerCase();

        if (biomeText.contains("desert") || biomeText.contains("badlands")) {
            return "narcotix:village/desert/cop_station_center";
        }
        if (biomeText.contains("savanna")) {
            return "narcotix:village/savanna/cop_station_center";
        }
        if (biomeText.contains("taiga")) {
            return "narcotix:village/taiga/cop_station_center";
        }
        if (biomeText.contains("snow") || biomeText.contains("frozen") || biomeText.contains("ice")) {
            return "narcotix:village/snowy/cop_station_center";
        }

        return "narcotix:village/plains/cop_station_center";
    }

    private static BlockPos findPlacementPos(ServerLevel level, BlockPos center) {
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (int radius = 16; radius <= SEARCH_RADIUS; radius += 6) {
            for (int dx = -radius; dx <= radius; dx += 6) {
                for (int dz = -radius; dz <= radius; dz += 6) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    BlockPos pos = surfacePos(level, x, z);

                    if (pos == null || !isFlatEnough(level, pos)) {
                        continue;
                    }

                    int distance = dx * dx + dz * dz;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }

            if (best != null) {
                return best;
            }
        }

        return null;
    }

    private static BlockPos surfacePos(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinY() + 1 || y >= level.getMaxY() - 8) {
            return null;
        }
        return new BlockPos(x, y, z);
    }

    private static boolean isFlatEnough(ServerLevel level, BlockPos pos) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (int dx = -HALF_FOOTPRINT; dx <= HALF_FOOTPRINT; dx += 3) {
            for (int dz = -HALF_FOOTPRINT; dz <= HALF_FOOTPRINT; dz += 3) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX() + dx, pos.getZ() + dz);
                min = Math.min(min, y);
                max = Math.max(max, y);

                if (max - min > 2) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean placeTemplate(MinecraftServer server, String templateId, BlockPos pos) {
        String command = "place template " + templateId + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();

        try {
            Object source = server.createCommandSourceStack();
            source = invokeNoArg(source, "withSuppressedOutput", source);
            source = invokeIntArg(source, "withPermission", 4, source);

            Object commands = server.getCommands();
            Method method = findCommandMethod(commands.getClass(), source.getClass());
            if (method == null) {
                NarcotixMod.LOGGER.warn("Could not find command execution method for Narcotix station placement.");
                return false;
            }

            method.invoke(commands, source, command);
            return true;
        } catch (Throwable throwable) {
            NarcotixMod.LOGGER.warn("Failed to place Narcotix police station with command '{}': {}", command, throwable.toString());
            return false;
        }
    }

    private static Method findCommandMethod(Class<?> commandsClass, Class<?> sourceClass) {
        Method[] methods = commandsClass.getMethods();
        for (Method method : methods) {
            if (!method.getName().equals("performPrefixedCommand")) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 2 && parameters[0].isAssignableFrom(sourceClass) && parameters[1] == String.class) {
                return method;
            }
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String methodName, Object fallback) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Object invokeIntArg(Object target, String methodName, int value, Object fallback) {
        try {
            Method method = target.getClass().getMethod(methodName, int.class);
            return method.invoke(target, value);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static void loadPlacedCells(MinecraftServer server) {
        if (loaded) {
            return;
        }
        loaded = true;

        Path path = dataPath(server);
        if (!Files.exists(path)) {
            return;
        }

        try {
            placedVillageCells.addAll(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            NarcotixMod.LOGGER.warn("Could not read Narcotix placed village stations file: {}", exception.toString());
        }
    }

    private static void savePlacedCells(MinecraftServer server) {
        Path path = dataPath(server);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, placedVillageCells, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            NarcotixMod.LOGGER.warn("Could not save Narcotix placed village stations file: {}", exception.toString());
        }
    }

    private static Path dataPath(MinecraftServer server) {
        try {
            Class<?> levelResourceClass = Class.forName("net.minecraft.world.level.storage.LevelResource");
            Object root = levelResourceClass.getField("ROOT").get(null);
            Method getWorldPath = server.getClass().getMethod("getWorldPath", levelResourceClass);
            Object result = getWorldPath.invoke(server, root);
            if (result instanceof Path) {
                return ((Path) result).resolve("data").resolve("narcotix_placed_village_stations.txt");
            }
        } catch (Throwable ignored) {
        }

        return Paths.get(".").resolve("narcotix_placed_village_stations.txt");
    }
}