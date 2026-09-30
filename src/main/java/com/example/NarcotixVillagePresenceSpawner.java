package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import java.lang.reflect.Field;
import java.util.Locale;

public final class NarcotixVillagePresenceSpawner {
    private static boolean registered = false;
    private static int ticks = 0;

    private static final int CHECK_EVERY_TICKS = 160;
    private static final double NEAR_PLAYER_RADIUS = 96.0D;
    private static final int MIN_SPAWN_DISTANCE = 18;
    private static final int MAX_SPAWN_DISTANCE = 56;
    private static final int ATTEMPTS_PER_PLAYER = 28;

    private static final int MAX_COPS_NEAR_PLAYER_IN_VILLAGE = 5;
    private static final int MAX_PLUGS_NEAR_PLAYER_IN_VILLAGE = 3;

    private NarcotixVillagePresenceSpawner() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(NarcotixVillagePresenceSpawner::tickServer);
    }

    private static void tickServer(MinecraftServer server) {
        ticks++;

        if (ticks < CHECK_EVERY_TICKS) {
            return;
        }

        ticks = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.level();

            if (level.dimension() != Level.OVERWORLD) {
                continue;
            }

            // Only do the heavier village spawner when the player is around village POIs/villagers.
            if (!isVillagePosition(level, player.blockPosition())) {
                continue;
            }

            int nearbyCops = level.getEntitiesOfClass(
                    CopEntity.class,
                    player.getBoundingBox().inflate(NEAR_PLAYER_RADIUS)
            ).size();

            int nearbyPlugs = level.getEntitiesOfClass(
                    WanderingPlugEntity.class,
                    player.getBoundingBox().inflate(NEAR_PLAYER_RADIUS)
            ).size();

            if (nearbyCops < getCopLimitForPoliceStations(countNearbyPoliceStations(level, player.blockPosition()))) {
                trySpawnCopNearPlayer(level, player);
            }

            if (nearbyPlugs < MAX_PLUGS_NEAR_PLAYER_IN_VILLAGE) {
                trySpawnPlugNearPlayer(level, player);
            }
        }
    }

    private static void trySpawnCopNearPlayer(ServerLevel level, ServerPlayer player) {
        BlockPos pos = findVillageSurfacePos(level, player);
        if (pos == null) {
            return;
        }

        CopEntity cop = new CopEntity(NarcotixCopAdditions.COP, level);
        placeEntity(level, cop, pos);
        level.addFreshEntity(cop);
    }

    private static void trySpawnPlugNearPlayer(ServerLevel level, ServerPlayer player) {
        BlockPos pos = findVillageSurfacePos(level, player);
        if (pos == null) {
            return;
        }

        WanderingPlugEntity plug = new WanderingPlugEntity(NarcotixEntities.WANDERING_PLUG, level);
        placeEntity(level, plug, pos);
        level.addFreshEntity(plug);
    }

    private static BlockPos findVillageSurfacePos(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();

        for (int attempt = 0; attempt < ATTEMPTS_PER_PLAYER; attempt++) {
            int distance = MIN_SPAWN_DISTANCE + random.nextInt(MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE + 1);
            double angle = random.nextDouble() * Math.PI * 2.0D;

            int x = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos pos = new BlockPos(x, y, z);

            if (!isVillagePosition(level, pos)) {
                continue;
            }

            if (!isGoodSurfaceSpawn(level, pos)) {
                continue;
            }

            return pos;
        }

        return null;
    }

    private static void placeEntity(ServerLevel level, net.minecraft.world.entity.Entity entity, BlockPos pos) {
        float yaw = level.getRandom().nextFloat() * 360.0F;
        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        entity.setYRot(yaw);
        entity.setXRot(0.0F);
    }

    private static boolean isGoodSurfaceSpawn(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinY() + 1 || pos.getY() >= level.getMaxY() - 2) {
            return false;
        }

        if (!level.canSeeSky(pos)) {
            return false;
        }

        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        if (!level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }

        if (!level.getFluidState(pos.below()).isEmpty()) {
            return false;
        }

        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static boolean isVillagePosition(ServerLevel level, BlockPos pos) {
        // Prefer the built-in village check if this mapping exposes it.
        try {
            Object result = level.getClass().getMethod("isVillage", BlockPos.class).invoke(level, pos);
            if (Boolean.TRUE.equals(result)) {
                return true;
            }
        } catch (Throwable ignored) {
            // Fall through to nearby-villager fallback.
        }

        // Fallback that still works in real villages with loaded villagers.
        return !level.getEntitiesOfClass(
                Villager.class,
                new AABB(pos).inflate(48.0D)
        ).isEmpty();
    }

    private static int getCopLimitForPoliceStations(int stationCount) {
        int limit = MAX_COPS_NEAR_PLAYER_IN_VILLAGE;

        for (int i = 0; i < stationCount; i++) {
            // Safety cap: still exponential, but prevents accidental hundreds of cops if many station chests are loaded.
            if (limit >= 80) {
                return 80;
            }
            limit *= 2;
        }

        return limit;
    }

    private static int countNearbyPoliceStations(ServerLevel level, BlockPos center) {
        int count = 0;
        int radius = 96;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -24, -radius), center.offset(radius, 24, radius))) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof ChestBlockEntity && isPoliceStationLootChest(blockEntity)) {
                count++;
            }
        }

        return count;
    }

    private static boolean isPoliceStationLootChest(BlockEntity blockEntity) {
        Class<?> current = blockEntity.getClass();

        while (current != null) {
            Field[] fields = current.getDeclaredFields();

            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(blockEntity);

                    if (value != null) {
                        String text = value.toString();
                        if (text.contains("narcotix") && text.contains("cop_station")) {
                            return true;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            current = current.getSuperclass();
        }

        return false;
    }
}