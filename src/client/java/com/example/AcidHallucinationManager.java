package com.example;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public final class AcidHallucinationManager {
    private static final Random RANDOM = new Random();
    private static final List<Hallucination> HALLUCINATIONS = new ArrayList<>();
    private static final List<SheepHallucination> SHEEP_HALLUCINATIONS = new ArrayList<>();
    private static boolean wasActive = false;
    private static int playerSpawnCooldown = 0;
    private static int sheepSpawnCooldown = 0;
    private static int nextEntityId = -100000;

    private static final DyeColor[] RAINBOW_COLORS = new DyeColor[] {
            DyeColor.RED,
            DyeColor.ORANGE,
            DyeColor.YELLOW,
            DyeColor.LIME,
            DyeColor.LIGHT_BLUE,
            DyeColor.BLUE,
            DyeColor.PURPLE,
            DyeColor.MAGENTA,
            DyeColor.PINK
    };

    private AcidHallucinationManager() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
        System.out.println("[Narcotix] Registered Acid hallucination manager v153 direct rainbow sheep fix.");
    }

    public static boolean isHallucination(Object entity) {
        for (Hallucination h : HALLUCINATIONS) {
            if (h.fake == entity) return true;
        }
        return false;
    }

    private static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            clear(level);
            wasActive = false;
            return;
        }

        boolean active = hasAcidVisuals(player);
        if (!active) {
            if (wasActive || !HALLUCINATIONS.isEmpty() || !SHEEP_HALLUCINATIONS.isEmpty()) {
                clear(level);
                System.out.println("[Narcotix] Acid hallucinations cleared.");
            }
            wasActive = false;
            return;
        }

        if (!wasActive) {
            System.out.println("[Narcotix] Acid hallucinations active; spawning fake people and direct rainbow sheep.");
            playerSpawnCooldown = 0;
            sheepSpawnCooldown = 5;
        }
        wasActive = true;

        if (playerSpawnCooldown-- <= 0 && HALLUCINATIONS.size() < 6) {
            spawnHallucination(player, level, HALLUCINATIONS.size());
            playerSpawnCooldown = 25 + RANDOM.nextInt(35);
        }

        if (sheepSpawnCooldown-- <= 0 && SHEEP_HALLUCINATIONS.size() < 5) {
            spawnRainbowSheep(player, level, SHEEP_HALLUCINATIONS.size());
            sheepSpawnCooldown = 25 + RANDOM.nextInt(45);
        }

        Iterator<Hallucination> it = HALLUCINATIONS.iterator();
        while (it.hasNext()) {
            Hallucination h = it.next();
            h.life--;
            if (h.life <= 0 || h.fake.isRemoved()) {
                safeRemove(level, h.fake);
                it.remove();
                continue;
            }
            updateHallucination(player, h);
        }

        Iterator<SheepHallucination> sit = SHEEP_HALLUCINATIONS.iterator();
        while (sit.hasNext()) {
            SheepHallucination h = sit.next();
            h.life--;
            if (h.life <= 0 || h.sheep.isRemoved()) {
                safeRemove(level, h.sheep);
                sit.remove();
                continue;
            }
            updateRainbowSheep(player, h);
        }
    }

    private static boolean hasAcidVisuals(LocalPlayer player) {
        if (player.hasEffect(MobEffects.NAUSEA)) return true;
        try {
            return player.hasEffect(NarcotixEffects.ACID);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void spawnHallucination(LocalPlayer player, ClientLevel level, int slot) {
        try {
            GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("narcotix-acid-hallucination-" + System.nanoTime()).getBytes()), "Steve");
            RemotePlayer fake = new RemotePlayer(level, profile);
            fake.setInvisible(false);
            fake.noPhysics = false;
            fake.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

            double angle = Math.toRadians(player.getYRot()) + (RANDOM.nextDouble() - 0.5D) * 1.8D;
            double dist = 4.0D + RANDOM.nextDouble() * 3.5D;
            double x = player.getX() - Math.sin(angle) * dist;
            double z = player.getZ() + Math.cos(angle) * dist;
            double y = player.getY();

            fake.setPos(x, y, z);
            facePlayer(fake, player);
            fake.setOnGround(true);
            fake.setShiftKeyDown(false);
            fake.setSprinting(false);
            fake.setNoGravity(false);

            int id = nextEntityId--;
            fake.setId(id);
            level.addEntity(fake);
            HALLUCINATIONS.add(new Hallucination(fake, 20 * 18 + RANDOM.nextInt(20 * 14), slot, angle));
        } catch (Throwable t) {
            System.out.println("[Narcotix] Failed to spawn acid hallucination: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Sheep createSheep(ClientLevel level) {
        Identifier id = Identifier.fromNamespaceAndPath("minecraft", "sheep");
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null) return null;
        return new Sheep((EntityType) type, level);
    }

    private static void spawnRainbowSheep(LocalPlayer player, ClientLevel level, int slot) {
        try {
            Sheep sheep = createSheep(level);
            if (sheep == null) {
                System.out.println("[Narcotix] Could not create rainbow sheep hallucination: sheep entity type was missing.");
                return;
            }

            sheep.setInvisible(false);
            sheep.noPhysics = false;
            sheep.setCustomName(null);
            sheep.setCustomNameVisible(false);
            sheep.setNoGravity(false);
            sheep.setSheared(false);

            double angle = Math.toRadians(player.getYRot()) + (RANDOM.nextDouble() - 0.5D) * 2.1D;
            double dist = 4.5D + RANDOM.nextDouble() * 4.0D;
            double x = player.getX() - Math.sin(angle) * dist;
            double z = player.getZ() + Math.cos(angle) * dist;
            double y = player.getY();

            sheep.setPos(x, y, z);
            faceEntity(sheep, player);
            sheep.setOnGround(true);
            sheep.setId(nextEntityId--);
            level.addEntity(sheep);

            SheepHallucination h = new SheepHallucination(sheep, 20 * 16 + RANDOM.nextInt(20 * 14), slot, angle);
            SHEEP_HALLUCINATIONS.add(h);
            setRainbowSheepColor(h);
            System.out.println("[Narcotix] Spawned direct rainbow sheep hallucination at " + x + ", " + y + ", " + z);
        } catch (Throwable t) {
            System.out.println("[Narcotix] Failed to spawn rainbow sheep hallucination: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static void updateHallucination(LocalPlayer player, Hallucination h) {
        h.age++;

        double orbit = h.baseAngle + Math.sin(h.age * 0.035D + h.slot) * 0.65D;
        double dist = 4.6D + Math.sin(h.age * 0.021D + h.slot * 2.0D) * 1.25D;
        double targetX = player.getX() - Math.sin(orbit) * dist;
        double targetZ = player.getZ() + Math.cos(orbit) * dist;
        double targetY = player.getY();

        double dx = targetX - h.fake.getX();
        double dz = targetZ - h.fake.getZ();
        double step = 0.035D;
        double moveX = clamp(dx, -step, step);
        double moveZ = clamp(dz, -step, step);

        h.fake.xo = h.fake.getX();
        h.fake.yo = h.fake.getY();
        h.fake.zo = h.fake.getZ();
        h.fake.setPos(h.fake.getX() + moveX, targetY, h.fake.getZ() + moveZ);
        h.fake.setDeltaMovement(moveX, 0.0D, moveZ);
        h.fake.setOnGround(true);
        h.fake.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        facePlayer(h.fake, player);
        tryUpdateWalkAnimation(h.fake, Math.sqrt(moveX * moveX + moveZ * moveZ));
    }

    private static void updateRainbowSheep(LocalPlayer player, SheepHallucination h) {
        h.age++;
        h.sheep.setCustomName(null);
        h.sheep.setCustomNameVisible(false);
        h.sheep.setSheared(false);

        double orbit = h.baseAngle + Math.sin(h.age * 0.028D + h.slot * 1.7D) * 0.85D;
        double dist = 5.2D + Math.sin(h.age * 0.018D + h.slot) * 1.7D;
        double targetX = player.getX() - Math.sin(orbit) * dist;
        double targetZ = player.getZ() + Math.cos(orbit) * dist;
        double targetY = player.getY();

        double dx = targetX - h.sheep.getX();
        double dz = targetZ - h.sheep.getZ();
        double step = 0.032D;
        double moveX = clamp(dx, -step, step);
        double moveZ = clamp(dz, -step, step);

        h.sheep.setPos(h.sheep.getX() + moveX, targetY, h.sheep.getZ() + moveZ);
        h.sheep.setDeltaMovement(moveX, 0.0D, moveZ);
        h.sheep.setOnGround(true);
        faceEntity(h.sheep, player);

        if (h.age % 5 == 0) {
            setRainbowSheepColor(h);
        }
    }

    private static void setRainbowSheepColor(SheepHallucination h) {
        DyeColor color = RAINBOW_COLORS[Math.floorMod((h.age / 5) + h.slot * 2, RAINBOW_COLORS.length)];
        h.sheep.setColor(color);
    }

    private static void facePlayer(RemotePlayer fake, LocalPlayer player) {
        double dx = player.getX() - fake.getX();
        double dz = player.getZ() - fake.getZ();
        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        fake.setYRot(yaw);
        fake.setYHeadRot(yaw);
        fake.yHeadRotO = yaw;
        fake.yBodyRot = yaw;
        fake.yBodyRotO = yaw;
        fake.setXRot(0.0F);
    }

    private static void faceEntity(Entity entity, LocalPlayer player) {
        double dx = player.getX() - entity.getX();
        double dz = player.getZ() - entity.getZ();
        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        entity.setYRot(yaw);
        entity.setXRot(0.0F);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void tryUpdateWalkAnimation(RemotePlayer fake, double speed) {
        trySetFloat(fake, "walkAnimationSpeed", (float)Math.min(1.0D, speed * 10.0D));
        trySetFloat(fake, "walkAnimationPosition", getFloat(fake, "walkAnimationPosition") + (float)(speed * 2.5D));
        trySetFloat(fake, "animationSpeed", (float)Math.min(1.0D, speed * 10.0D));
        trySetFloat(fake, "animationPosition", getFloat(fake, "animationPosition") + (float)(speed * 2.5D));
    }

    private static float getFloat(Object obj, String name) {
        try {
            Field f = findField(obj.getClass(), name);
            if (f == null) return 0.0F;
            f.setAccessible(true);
            return f.getFloat(obj);
        } catch (Throwable ignored) {
            return 0.0F;
        }
    }

    private static void trySetFloat(Object obj, String name, float value) {
        try {
            Field f = findField(obj.getClass(), name);
            if (f == null) return;
            f.setAccessible(true);
            f.setFloat(obj, value);
        } catch (Throwable ignored) {
        }
    }

    private static Field findField(Class<?> cls, String name) {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static void clear(ClientLevel level) {
        if (level != null) {
            for (Hallucination h : HALLUCINATIONS) {
                safeRemove(level, h.fake);
            }
            for (SheepHallucination h : SHEEP_HALLUCINATIONS) {
                safeRemove(level, h.sheep);
            }
        }
        HALLUCINATIONS.clear();
        SHEEP_HALLUCINATIONS.clear();
    }

    private static void safeRemove(ClientLevel level, Entity entity) {
        try {
            entity.discard();
        } catch (Throwable ignored) {
            try { entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED); } catch (Throwable ignored2) {}
        }
    }

    private static final class Hallucination {
        final RemotePlayer fake;
        int life;
        final int slot;
        final double baseAngle;
        int age;

        Hallucination(RemotePlayer fake, int life, int slot, double baseAngle) {
            this.fake = fake;
            this.life = life;
            this.slot = slot;
            this.baseAngle = baseAngle;
            this.age = RANDOM.nextInt(80);
        }
    }

    private static final class SheepHallucination {
        final Sheep sheep;
        int life;
        final int slot;
        final double baseAngle;
        int age;

        SheepHallucination(Sheep sheep, int life, int slot, double baseAngle) {
            this.sheep = sheep;
            this.life = life;
            this.slot = slot;
            this.baseAngle = baseAngle;
            this.age = RANDOM.nextInt(80);
        }
    }
}