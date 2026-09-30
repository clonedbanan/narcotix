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

import java.util.HashSet;
import java.util.Set;

public final class NarcotixAcidCrystalGenerator {
    private static boolean registered = false;
    private static int ticks = 0;

    private static final int CHECK_EVERY_TICKS = 200;
    private static final int CHUNK_RADIUS = 2;
    private static final int ATTEMPTS_PER_CHUNK = 6;
    private static final int MIN_Y = -58;
    private static final int MAX_Y = 4;

    private static final Set<String> checkedCells = new HashSet<>();

    private NarcotixAcidCrystalGenerator() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(NarcotixAcidCrystalGenerator::tickServer);
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
            generateAround(level, player.blockPosition());
        }
    }

    private static void generateAround(ServerLevel level, BlockPos origin) {
        int centerChunkX = origin.getX() >> 4;
        int centerChunkZ = origin.getZ() >> 4;
        RandomSource random = level.getRandom();

        for (int chunkX = centerChunkX - CHUNK_RADIUS; chunkX <= centerChunkX + CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = centerChunkZ - CHUNK_RADIUS; chunkZ <= centerChunkZ + CHUNK_RADIUS; chunkZ++) {
                String key = level.dimension().toString() + ":" + chunkX + ":" + chunkZ;
                if (checkedCells.contains(key)) {
                    continue;
                }
                checkedCells.add(key);
                tryPlaceInChunk(level, chunkX, chunkZ, random);
            }
        }
    }

    private static void tryPlaceInChunk(ServerLevel level, int chunkX, int chunkZ, RandomSource random) {
        for (int attempt = 0; attempt < ATTEMPTS_PER_CHUNK; attempt++) {
            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);
            int y = MIN_Y + random.nextInt(MAX_Y - MIN_Y + 1);
            BlockPos pos = new BlockPos(x, y, z);

            if (!canPlaceAcidCrystal(level, pos)) {
                continue;
            }

            level.setBlock(pos, NarcotixMod.LSD_CLUSTER.defaultBlockState(), 3);
            return;
        }
    }

    private static boolean canPlaceAcidCrystal(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinY() + 2 || pos.getY() >= level.getMaxY() - 2) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            return false;
        }

        if (level.canSeeSky(pos)) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            if (isDeepslateLike(level.getBlockState(pos.relative(direction)))) {
                return true;
            }
        }

        return false;
    }

    private static boolean isDeepslateLike(BlockState state) {
        if (state.isAir()) {
            return false;
        }

        try {
            String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            return id.contains("deepslate");
        } catch (Throwable ignored) {
            return false;
        }
    }
}