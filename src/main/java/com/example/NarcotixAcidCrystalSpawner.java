package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class NarcotixAcidCrystalSpawner {
    private static boolean registered = false;
    private static int ticks = 0;

    private static final int CHECK_EVERY_TICKS = 40;
    private static final int ATTEMPTS_PER_PLAYER = 96;
    private static final int HORIZONTAL_RADIUS = 36;
    private static final int VERTICAL_RADIUS = 18;
    private static final int MAX_DEEPSLATE_Y = 8;

    private NarcotixAcidCrystalSpawner() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(NarcotixAcidCrystalSpawner::tickServer);
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

            for (int attempt = 0; attempt < ATTEMPTS_PER_PLAYER; attempt++) {
                if (tryPlaceNearPlayer(level, player)) {
                    break;
                }
            }
        }
    }

    private static boolean tryPlaceNearPlayer(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        BlockPos playerPos = player.blockPosition();

        int x = playerPos.getX() + random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
        int y = playerPos.getY() + random.nextInt(VERTICAL_RADIUS * 2 + 1) - VERTICAL_RADIUS;
        int z = playerPos.getZ() + random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;

        int minY = level.getMinY() + 4;
        int maxY = Math.min(MAX_DEEPSLATE_Y, level.getMaxY() - 4);
        if (y < minY) {
            y = minY;
        }
        if (y > maxY) {
            y = maxY;
        }

        BlockPos crystalPos = new BlockPos(x, y, z);
        if (!level.getBlockState(crystalPos).isAir()) {
            return false;
        }

        Direction[] directions = Direction.values();
        Direction facing = directions[random.nextInt(directions.length)];
        BlockPos supportPos = crystalPos.relative(facing.getOpposite());
        BlockState supportState = level.getBlockState(supportPos);

        if (!isDeepslateLike(supportState)) {
            return false;
        }

        BlockState crystalState = NarcotixMod.LSD_CLUSTER.defaultBlockState().setValue(AcidCrystalBlock.FACING, facing);
        return level.setBlock(crystalPos, crystalState, 3);
    }

    private static boolean isDeepslateLike(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return id.contains("deepslate") || id.equals("minecraft:tuff");
    }
}