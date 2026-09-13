package com.example;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

public class NarcotixDrugOverlay {
    private static final long COKE_HEARTBEAT_MS = 620L;
    private static long lastCokeBeat = -1L;

    private static double lastX = 0.0D;
    private static double lastZ = 0.0D;
    private static double motionMemory = 0.0D;

    public static void render(Object graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || graphics == null) {
            return;
        }

        Player player = minecraft.player;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        long now = System.currentTimeMillis();

        if (player.hasEffect(NarcotixEffects.COKE_RUSH)) {
            renderCokeRush(graphics, minecraft, player, width, height, now);
        } else {
            lastCokeBeat = -1L;
        }

        if (player.hasEffect(NarcotixEffects.WEED_HIGH)) {
            renderWeedHigh(graphics, width, height, now, player);
        } else {
            motionMemory = 0.0D;
            lastX = player.getX();
            lastZ = player.getZ();
        }

        if (player.hasEffect(NarcotixEffects.TOBACCO)) {
            renderTobacco(graphics, width, height, now);
        }
    }

    private static void renderCokeRush(Object graphics, Minecraft minecraft, Player player, int width, int height, long now) {
        long beat = now / COKE_HEARTBEAT_MS;
        long beatStart = beat * COKE_HEARTBEAT_MS;
        long elapsed = now - beatStart;

        if (beat != lastCokeBeat) {
            lastCokeBeat = beat;
            playHeartbeat(minecraft, player);
        }

        double main = Math.exp(-Math.pow(elapsed / 105.0D, 2.0D));
        double echo = Math.exp(-Math.pow((elapsed - 170L) / 85.0D, 2.0D)) * 0.45D;
        double pulse = clamp(main + echo, 0.0D, 1.0D);

        int edgeAlpha = (int)(24.0D + pulse * 125.0D);
        int deepAlpha = (int)(10.0D + pulse * 58.0D);
        int thinAlpha = (int)(18.0D + pulse * 92.0D);

        int edgeColor = argb(edgeAlpha, 160, 0, 0);
        int deepColor = argb(deepAlpha, 95, 0, 0);
        int thinColor = argb(thinAlpha, 255, 10, 10);

        int side = Math.max(18, width / 9);
        int top = Math.max(14, height / 10);
        int bottom = Math.max(20, height / 8);

        fill(graphics, 0, 0, width, top, edgeColor);
        fill(graphics, 0, height - bottom, width, height, edgeColor);
        fill(graphics, 0, 0, side, height, edgeColor);
        fill(graphics, width - side, 0, width, height, edgeColor);

        int innerSide = Math.max(8, side / 2);
        int innerTop = Math.max(6, top / 2);
        fill(graphics, innerSide, innerTop, width - innerSide, innerTop + 2, thinColor);
        fill(graphics, innerSide, height - innerTop - 2, width - innerSide, height - innerTop, thinColor);
        fill(graphics, innerSide, innerTop, innerSide + 2, height - innerTop, thinColor);
        fill(graphics, width - innerSide - 2, innerTop, width - innerSide, height - innerTop, thinColor);

        int corner = Math.max(30, Math.min(width, height) / 5);
        fill(graphics, 0, 0, corner, corner, deepColor);
        fill(graphics, width - corner, 0, width, corner, deepColor);
        fill(graphics, 0, height - corner, corner, height, deepColor);
        fill(graphics, width - corner, height - corner, width, height, deepColor);
    }

    private static void playHeartbeat(Minecraft minecraft, Player player) {
        if (minecraft == null || minecraft.level == null || player == null) {
            return;
        }
        try {
            minecraft.level.playLocalSound(
                    player.getX(), player.getY(), player.getZ(),
                    NarcotixMod.COCAINE_HEARTBEAT_SOUND,
                    SoundSource.PLAYERS,
                    0.75F,
                    1.0F,
                    false
            );
        } catch (Throwable ignored) {
            try {
                player.playSound(NarcotixMod.COCAINE_HEARTBEAT_SOUND, 0.75F, 1.0F);
            } catch (Throwable ignoredAgain) {
            }
        }
    }

    private static void renderWeedHigh(Object graphics, int width, int height, long now, Player player) {
        double dx = player.getX() - lastX;
        double dz = player.getZ() - lastZ;
        lastX = player.getX();
        lastZ = player.getZ();

        double motion = Math.min(1.0D, Math.sqrt(dx * dx + dz * dz) * 9.0D);
        motionMemory = Math.max(motion, motionMemory * 0.90D);

        double slow = (Math.sin(now / 720.0D) + 1.0D) * 0.5D;
        double drift = Math.sin(now / 360.0D) * 0.5D + 0.5D;

        fill(graphics, 0, 0, width, height, argb(22, 220, 145, 35));
        fill(graphics, 0, 0, width, height, argb((int)(10 + slow * 10), 70, 155, 50));

        int trailAlpha = (int)(12 + motionMemory * 34);
        int driftX = (int)(Math.sin(now / 240.0D) * (4 + motionMemory * 14));
        int driftY = (int)(Math.cos(now / 310.0D) * (3 + motionMemory * 10));

        fill(graphics, driftX - 22, driftY, width + driftX, height + driftY, argb(trailAlpha, 255, 130, 25));
        fill(graphics, -driftX, -driftY - 16, width - driftX + 18, height - driftY, argb((int)(trailAlpha * 0.55D), 40, 210, 90));

        int bandCount = 9;
        for (int i = 0; i < bandCount; i++) {
            int y = (int)(((now / 25L) + i * (height / bandCount + 11)) % (height + 40)) - 20;
            int bandAlpha = (i % 2 == 0) ? 12 : 8;
            fill(graphics, 0, y, width, y + 2, argb(bandAlpha, 255, 185, 75));
        }

        for (int i = 0; i < 18; i++) {
            int x = (int)((i * 97L + now / 7L) % Math.max(1, width));
            int y = (int)((i * 53L + now / 11L) % Math.max(1, height));
            int size = 1 + (i % 3);
            int alpha = (int)(10 + drift * 12);
            fill(graphics, x, y, x + size, y + size, argb(alpha, 255, 220, 115));
        }

        int vignetteAlpha = 32;
        int edge = Math.max(22, Math.min(width, height) / 8);
        fill(graphics, 0, 0, width, edge, argb(vignetteAlpha, 55, 35, 0));
        fill(graphics, 0, height - edge, width, height, argb(vignetteAlpha, 55, 35, 0));
        fill(graphics, 0, 0, edge, height, argb(vignetteAlpha, 55, 35, 0));
        fill(graphics, width - edge, 0, width, height, argb(vignetteAlpha, 55, 35, 0));
    }

    private static void renderTobacco(Object graphics, int width, int height, long now) {
        double pulse = (Math.sin(now / 1300.0D) + 1.0D) * 0.5D;
        int warmAlpha = (int)(12.0D + pulse * 8.0D);
        int edgeAlpha = (int)(16.0D + pulse * 8.0D);
        int edge = Math.max(18, Math.min(width, height) / 10);

        // Very subtle warm tobacco hue. No trails, no fuzzy weed distortion.
        fill(graphics, 0, 0, width, height, argb(warmAlpha, 210, 145, 78));
        fill(graphics, 0, 0, width, edge, argb(edgeAlpha, 95, 58, 28));
        fill(graphics, 0, height - edge, width, height, argb(edgeAlpha, 95, 58, 28));
        fill(graphics, 0, 0, edge, height, argb(edgeAlpha, 95, 58, 28));
        fill(graphics, width - edge, 0, width, height, argb(edgeAlpha, 95, 58, 28));
    }

    private static int argb(int a, int r, int g, int b) {
        a = Math.max(0, Math.min(255, a));
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void fill(Object graphics, int x1, int y1, int x2, int y2, int color) {
        if (graphics == null) {
            return;
        }
        try {
            Method[] methods = graphics.getClass().getMethods();
            for (Method method : methods) {
                if (!method.getName().equals("fill")) {
                    continue;
                }
                Class<?>[] p = method.getParameterTypes();
                if (p.length == 5
                        && p[0] == int.class
                        && p[1] == int.class
                        && p[2] == int.class
                        && p[3] == int.class
                        && p[4] == int.class) {
                    method.invoke(graphics, x1, y1, x2, y2, color);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}