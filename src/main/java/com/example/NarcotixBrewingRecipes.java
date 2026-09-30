package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class NarcotixBrewingRecipes {
    private static boolean registered = false;

    private static final int BREW_TIME_TICKS = 400;
    private static final int SCAN_RADIUS_XZ = 10;
    private static final int SCAN_RADIUS_Y = 6;
    private static final int BLAZE_POWDER_FUEL_VALUE = 20;

    private static final Map<String, Integer> BREW_TIMERS = new HashMap<>();

    private NarcotixBrewingRecipes() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(NarcotixBrewingRecipes::tickServer);
        System.out.println("[Narcotix] Registered LSD shard brewing stand handler.");
    }

    private static void tickServer(MinecraftServer server) {
        boolean sawAnyStand = false;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.level();
            if (level.dimension() != Level.OVERWORLD) {
                continue;
            }
            if (scanNearPlayer(level, player.blockPosition())) {
                sawAnyStand = true;
            }
        }

        if (!sawAnyStand && !BREW_TIMERS.isEmpty()) {
            BREW_TIMERS.clear();
        } else if (!BREW_TIMERS.isEmpty()) {
            cleanupUnseenTimers();
        }
    }

    private static boolean scanNearPlayer(ServerLevel level, BlockPos center) {
        boolean sawStand = false;
        BlockPos min = center.offset(-SCAN_RADIUS_XZ, -SCAN_RADIUS_Y, -SCAN_RADIUS_XZ);
        BlockPos max = center.offset(SCAN_RADIUS_XZ, SCAN_RADIUS_Y, SCAN_RADIUS_XZ);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BrewingStandBlockEntity && blockEntity instanceof Container container) {
                sawStand = true;
                processBrewingStand(level, pos.immutable(), container);
            }
        }
        return sawStand;
    }

    private static void processBrewingStand(ServerLevel level, BlockPos pos, Container container) {
        String key = brewingKey(level, pos);
        boolean active = BREW_TIMERS.containsKey(key);

        if (!hasLsdShardIngredient(container) || !hasBrewableBottle(container)) {
            BREW_TIMERS.remove(key);
            setVanillaBrewTime(container, 0);
            container.setChanged();
            return;
        }

        if (!active) {
            if (!consumeOneBrewingFuel(container)) {
                // No fuel means no fake progress animation. This prevents the white bar from flashing
                // when the stand has an LSD shard but no blaze powder/fuel.
                setVanillaBrewTime(container, 0);
                container.setChanged();
                return;
            }
            BREW_TIMERS.put(key, 0);
            active = true;
        }

        int progress = BREW_TIMERS.getOrDefault(key, 0) + 1;
        int remaining = Math.max(1, BREW_TIME_TICKS - progress);
        setVanillaBrewTime(container, remaining);

        if (progress < BREW_TIME_TICKS) {
            BREW_TIMERS.put(key, progress);
            container.setChanged();
            return;
        }

        boolean brewedAny = false;
        for (int slot = 0; slot < 3 && slot < container.getContainerSize(); slot++) {
            ItemStack bottle = container.getItem(slot);
            if (isBrewableBottle(bottle)) {
                container.setItem(slot, new ItemStack(NarcotixMod.BOTTLE_OF_PURE_ACID));
                brewedAny = true;
            }
        }

        if (brewedAny) {
            ItemStack ingredient = container.getItem(3);
            ingredient.shrink(1);
            if (ingredient.isEmpty()) {
                container.setItem(3, ItemStack.EMPTY);
            } else {
                container.setItem(3, ingredient);
            }
            level.levelEvent(1035, pos, 0);
        }

        BREW_TIMERS.remove(key);
        setVanillaBrewTime(container, 0);
        container.setChanged();
    }

    private static boolean consumeOneBrewingFuel(Container container) {
        int fuel = getVanillaFuel(container);

        if (fuel <= 0) {
            if (container.getContainerSize() <= 4) {
                return false;
            }

            ItemStack fuelStack = container.getItem(4);
            if (fuelStack.isEmpty() || !fuelStack.is(Items.BLAZE_POWDER)) {
                return false;
            }

            fuelStack.shrink(1);
            if (fuelStack.isEmpty()) {
                container.setItem(4, ItemStack.EMPTY);
            } else {
                container.setItem(4, fuelStack);
            }
            fuel = BLAZE_POWDER_FUEL_VALUE;
        }

        setVanillaFuel(container, Math.max(0, fuel - 1));
        container.setChanged();
        return true;
    }

    private static boolean hasLsdShardIngredient(Container container) {
        if (container.getContainerSize() <= 3) {
            return false;
        }
        ItemStack ingredient = container.getItem(3);
        return !ingredient.isEmpty() && ingredient.is(NarcotixMod.LSD_SHARD);
    }

    private static boolean hasBrewableBottle(Container container) {
        for (int slot = 0; slot < 3 && slot < container.getContainerSize(); slot++) {
            if (isBrewableBottle(container.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBrewableBottle(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.POTION);
    }

    private static String brewingKey(ServerLevel level, BlockPos pos) {
        return String.valueOf(level.dimension()) + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    private static void cleanupUnseenTimers() {
        // Keep the map from growing forever if a player walks away mid-brew.
        // Active stands will be refreshed by processBrewingStand every tick.
        if (BREW_TIMERS.size() > 256) {
            Iterator<String> iterator = BREW_TIMERS.keySet().iterator();
            while (BREW_TIMERS.size() > 128 && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    private static int getVanillaFuel(Container container) {
        Integer value = getIntField(container, "fuel");
        return value == null ? 0 : value;
    }

    private static void setVanillaFuel(Container container, int value) {
        setIntField(container, "fuel", value);
    }

    private static void setVanillaBrewTime(Container container, int value) {
        setIntField(container, "brewTime", value);
    }

    private static Integer getIntField(Object target, String fieldName) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (field.getType() == int.class) {
                    field.setAccessible(true);
                    return field.getInt(target);
                }
            } catch (Throwable ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static void setIntField(Object target, String fieldName, int value) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (field.getType() == int.class) {
                    field.setAccessible(true);
                    field.setInt(target, value);
                    return;
                }
            } catch (Throwable ignored) {
            }
            current = current.getSuperclass();
        }
    }
}