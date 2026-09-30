package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class NarcotixAcidBrewingStandHandler {
    private static boolean registered = false;
    private static int ticks = 0;

    private static final int CHECK_EVERY_TICKS = 20;
    private static final int SCAN_RADIUS = 10;

    private NarcotixAcidBrewingStandHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(NarcotixAcidBrewingStandHandler::tickServer);
    }

    private static void tickServer(MinecraftServer server) {
        ticks++;
        if (ticks < CHECK_EVERY_TICKS) {
            return;
        }
        ticks = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.level();
            scanAroundPlayer(level, player.blockPosition());
        }
    }

    private static void scanAroundPlayer(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                center.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS))) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!isBrewingStand(blockEntity)) {
                continue;
            }
            if (!(blockEntity instanceof Container container)) {
                continue;
            }
            processBrewingStand(container);
        }
    }

    private static boolean isBrewingStand(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        String id = String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
        return "minecraft:brewing_stand".equals(id);
    }

    private static void processBrewingStand(Container container) {
        if (container.getContainerSize() < 4) {
            return;
        }

        ItemStack ingredient = container.getItem(3);
        if (ingredient.isEmpty() || !ingredient.is(NarcotixMod.LSD_CLUSTER.asItem())) {
            return;
        }

        boolean brewed = false;
        for (int slot = 0; slot < 3; slot++) {
            ItemStack bottle = container.getItem(slot);
            if (bottle.isEmpty()) {
                continue;
            }

            if (bottle.is(Items.POTION) || bottle.is(Items.GLASS_BOTTLE)) {
                container.setItem(slot, new ItemStack(NarcotixMod.BOTTLE_OF_PURE_ACID));
                brewed = true;
            }
        }

        if (brewed) {
            ingredient.shrink(1);
            container.setItem(3, ingredient.isEmpty() ? ItemStack.EMPTY : ingredient);
            container.setChanged();
        }
    }
}