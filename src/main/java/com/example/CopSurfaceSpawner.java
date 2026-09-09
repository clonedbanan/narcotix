package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CopSurfaceSpawner {
    private static boolean registered = false;
    private static int ticks = 0;

    private static final int CHECK_EVERY_TICKS = 200;
    private static final int MAX_COPS_NEAR_PLAYER = 3;
    private static final double NEAR_PLAYER_RADIUS = 96.0D;
    private static final int MIN_SPAWN_DISTANCE = 28;
    private static final int MAX_SPAWN_DISTANCE = 72;
    private static final int ATTEMPTS_PER_PLAYER = 18;

    private CopSurfaceSpawner() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(CopSurfaceSpawner::tickServer);
    }

    private static void tickServer(MinecraftServer server) {
        ticks++;

        if (ticks < CHECK_EVERY_TICKS) {
            return;
        }

        ticks = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.level();

            // NARCOTIX_OUTSIDE_SPAWN_THROTTLE_V23
            // Keep non-village surface spawns much rarer while leaving village spawns alone.
            if (level.getRandom().nextInt(10) != 0) {
                continue;
            }

            if (level.dimension() != Level.OVERWORLD) {
                continue;
            }

            int nearbyCops = level.getEntitiesOfClass(
                    CopEntity.class,
                    player.getBoundingBox().inflate(NEAR_PLAYER_RADIUS)
            ).size();

            if (nearbyCops >= MAX_COPS_NEAR_PLAYER) {
                continue;
            }

            trySpawnNearPlayer(level, player);
        }
    }

    private static void trySpawnNearPlayer(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();

        for (int attempt = 0; attempt < ATTEMPTS_PER_PLAYER; attempt++) {
            int distance = MIN_SPAWN_DISTANCE + random.nextInt(MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE + 1);
            double angle = random.nextDouble() * Math.PI * 2.0D;

            int x = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos pos = new BlockPos(x, y, z);

            if (!isGoodSurfaceSpawn(level, pos)) {
                continue;
            }

            CopEntity cop = new CopEntity(NarcotixCopAdditions.COP, level);
            float yaw = level.getRandom().nextFloat() * 360.0F;
cop.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
cop.setYRot(yaw);
cop.setXRot(0.0F);

            level.addFreshEntity(cop);
            return;
        }
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
}