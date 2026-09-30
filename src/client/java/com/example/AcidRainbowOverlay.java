package com.example;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;

public final class AcidRainbowOverlay {
    private static final Map<Class<?>, Object> PIPELINE_CACHE = new HashMap<>();
    private static boolean registered = false;
    private static boolean loggedHudElement = false;
    private static boolean loggedHudCallback = false;
    private static boolean loggedDraw = false;
    private static long lastStateDebugMs = 0L;

    private AcidRainbowOverlay() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        registerHudElementRegistry();
        registerOldHudRenderCallback();
        registerClientTickDebug();
    }

    private static void registerHudElementRegistry() {
        try {
            Class<?> registryClass = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry");
            Class<?> elementClass = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement");

            Method addLast = findStaticTwoArgMethod(registryClass, "addLast", elementClass);
            if (addLast == null) {
                System.out.println("[Narcotix] Acid overlay skipped HudElementRegistry: addLast was not found.");
                return;
            }

            Object id = makeIdentifierForType(addLast.getParameterTypes()[0], "acid_rainbow_overlay");
            Object hudElement = Proxy.newProxyInstance(
                    elementClass.getClassLoader(),
                    new Class<?>[] { elementClass },
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            if ("toString".equals(method.getName())) return "Narcotix Acid Rainbow Overlay";
                            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                            if ("equals".equals(method.getName())) return proxy == args[0];
                        }
                        if (args != null && args.length > 0 && isProbablyDrawContext(args[0])) {
                            render(args[0]);
                        }
                        return null;
                    });

            addLast.invoke(null, id, hudElement);
            loggedHudElement = true;
            System.out.println("[Narcotix] Registered Acid rainbow overlay v114 through HudElementRegistry.");
        } catch (Throwable throwable) {
            System.out.println("[Narcotix] Acid HudElementRegistry overlay registration failed: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private static void registerOldHudRenderCallback() {
        try {
            Class<?> callbackClass = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback");
            Object event = callbackClass.getField("EVENT").get(null);
            Method register = findInstanceRegister(event.getClass());
            if (register == null) {
                System.out.println("[Narcotix] Acid overlay skipped HudRenderCallback: EVENT.register was not found.");
                return;
            }

            Object listener = Proxy.newProxyInstance(
                    callbackClass.getClassLoader(),
                    new Class<?>[] { callbackClass },
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            if ("toString".equals(method.getName())) return "Narcotix Acid Old HUD Callback";
                            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                            if ("equals".equals(method.getName())) return proxy == args[0];
                        }
                        if (args != null && args.length > 0 && isProbablyDrawContext(args[0])) {
                            render(args[0]);
                        }
                        return null;
                    });

            register.invoke(event, listener);
            loggedHudCallback = true;
            System.out.println("[Narcotix] Registered Acid rainbow overlay v114 through HudRenderCallback fallback.");
        } catch (ClassNotFoundException ignored) {
            System.out.println("[Narcotix] HudRenderCallback fallback not present; using reflected HudElementRegistry only.");
        } catch (Throwable throwable) {
            System.out.println("[Narcotix] Acid HudRenderCallback fallback registration failed: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private static void registerClientTickDebug() {
        try {
            Class<?> eventsClass = Class.forName("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents");
            Object endTick = eventsClass.getField("END_CLIENT_TICK").get(null);
            Method register = findInstanceRegister(endTick.getClass());
            if (register == null) return;

            Class<?> listenerClass = register.getParameterTypes()[0];
            Object listener = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[] { listenerClass },
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            if ("toString".equals(method.getName())) return "Narcotix Acid Client Tick Debug";
                            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                            if ("equals".equals(method.getName())) return proxy == args[0];
                        }
                        debugStateOccasionally();
                        return null;
                    });

            register.invoke(endTick, listener);
            System.out.println("[Narcotix] Registered Acid overlay v114 client tick state check.");
        } catch (Throwable throwable) {
            System.out.println("[Narcotix] Acid overlay client tick state check failed: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private static Method findStaticTwoArgMethod(Class<?> owner, String name, Class<?> secondArgClass) {
        for (Method method : owner.getMethods()) {
            if (!name.equals(method.getName())) continue;
            if (!Modifier.isStatic(method.getModifiers())) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 2 && params[1].isAssignableFrom(secondArgClass)) return method;
            if (params.length == 2 && secondArgClass.isAssignableFrom(params[1])) return method;
        }
        return null;
    }

    private static Method findInstanceRegister(Class<?> owner) {
        for (Method method : owner.getMethods()) {
            if (!"register".equals(method.getName())) continue;
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (method.getParameterTypes().length == 1) return method;
        }
        return null;
    }

    private static Object makeIdentifierForType(Class<?> idType, String path) throws Exception {
        try {
            Method factory = idType.getMethod("fromNamespaceAndPath", String.class, String.class);
            return factory.invoke(null, NarcotixMod.MOD_ID, path);
        } catch (NoSuchMethodException ignored) {
            Constructor<?> constructor = idType.getConstructor(String.class, String.class);
            return constructor.newInstance(NarcotixMod.MOD_ID, path);
        }
    }

    private static boolean isProbablyDrawContext(Object object) {
        if (object == null) return false;
        String name = object.getClass().getName().toLowerCase(Locale.ROOT);
        return name.contains("gui") || name.contains("draw") || hasFillMethod(object.getClass());
    }

    private static boolean hasFillMethod(Class<?> cls) {
        Class<?> current = cls;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if ("fill".equals(method.getName())) return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static void render(Object drawContext) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            loggedDraw = false;
            return;
        }

        if (!isAcidActive(minecraft)) {
            loggedDraw = false;
            return;
        }

        int width = getWindowInt(minecraft, "getGuiScaledWidth", "getWidth");
        int height = getWindowInt(minecraft, "getGuiScaledHeight", "getHeight");
        if (width <= 0 || height <= 0) return;

        enableBlend();

        long now = System.currentTimeMillis();
        float baseHue = (now % 6500L) / 6500.0F;

        // Dimmer than v112: gentle wash, dim moving bands, very light edge glow.
        fill(drawContext, 0, 0, width, height, hsvToArgb(baseHue, 0.80F, 1.00F, 0x20));

        int bandHeight = Math.max(16, height / 9);
        int travel = height + bandHeight * 2;
        for (int i = 0; i < 7; i++) {
            float hue = baseHue + (i * 0.145F);
            int color = hsvToArgb(hue, 0.95F, 1.0F, 0x18);
            int offset = (int) (((now / 18L) + (i * travel / 7)) % travel) - bandHeight;
            fill(drawContext, 0, offset, width, Math.min(height, offset + Math.max(6, bandHeight / 2)), color);
        }

        int edgeSize = Math.max(14, Math.min(width, height) / 18);
        fill(drawContext, 0, 0, width, edgeSize, hsvToArgb(baseHue + 0.20F, 0.85F, 1.0F, 0x16));
        fill(drawContext, 0, height - edgeSize, width, height, hsvToArgb(baseHue + 0.45F, 0.85F, 1.0F, 0x16));
        fill(drawContext, 0, 0, edgeSize, height, hsvToArgb(baseHue + 0.70F, 0.85F, 1.0F, 0x14));
        fill(drawContext, width - edgeSize, 0, width, height, hsvToArgb(baseHue + 0.95F, 0.85F, 1.0F, 0x14));

        if (!loggedDraw) {
            loggedDraw = true;
            System.out.println("[Narcotix] Acid rainbow overlay v114 drawing only while Acid/Nausea is active. HudElement=" + loggedHudElement + ", HudRenderCallback=" + loggedHudCallback + ".");
        }
    }

    private static boolean isAcidActive(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) return false;
        Object player = minecraft.player;

        Object acid = getStaticFieldValue("com.example.NarcotixEffects", "ACID");
        if (hasEffect(player, acid)) return true;

        try {
            if (hasEffect(player, MobEffects.NAUSEA)) return true;
        } catch (Throwable ignored) {
        }

        try {
            Collection<?> effects = minecraft.player.getActiveEffects();
            for (Object instance : effects) {
                if (!effectInstanceStillActive(instance)) continue;
                String key = effectRegistryKey(instance).toLowerCase(Locale.ROOT);
                if (key.contains("narcotix:acid") || key.contains("minecraft:nausea") || key.contains("minecraft:confusion")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    private static Object getStaticFieldValue(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasEffect(Object player, Object effect) {
        if (player == null || effect == null) return false;
        try {
            for (Method method : player.getClass().getMethods()) {
                if (!"hasEffect".equals(method.getName()) || method.getParameterCount() != 1) continue;
                Class<?> parameterType = method.getParameterTypes()[0];
                if (!parameterType.isAssignableFrom(effect.getClass())) continue;
                Object result = method.invoke(player, effect);
                return result instanceof Boolean && (Boolean) result;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean effectInstanceStillActive(Object instance) {
        try {
            Object duration = callNoArg(instance, "getDuration");
            if (duration instanceof Number) return ((Number) duration).intValue() > 0;
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static String effectRegistryKey(Object instance) {
        Object holder = callNoArg(instance, "getEffect");
        if (holder == null) holder = callNoArg(instance, "getMobEffect");
        Object value = holder;
        Object holderValue = callNoArg(holder, "value");
        if (holderValue != null) value = holderValue;
        String key = registryKeyForMobEffect(value);
        if (!"unknown".equals(key)) return key;
        return String.valueOf(instance);
    }

    private static Object callNoArg(Object object, String methodName) {
        if (object == null) return null;
        try {
            Method method = object.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(object);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String registryKeyForMobEffect(Object effectValue) {
        try {
            Class<?> registries = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            for (Field field : registries.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) continue;
                String name = field.getName().toUpperCase(Locale.ROOT);
                if (!name.contains("MOB_EFFECT")) continue;
                field.setAccessible(true);
                Object registry = field.get(null);
                if (registry == null) continue;
                for (String methodName : new String[] { "getKey", "getId" }) {
                    try {
                        Method method = registry.getClass().getMethod(methodName, Object.class);
                        Object key = method.invoke(registry, effectValue);
                        if (key != null) return String.valueOf(key);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return "unknown";
    }

    private static int getWindowInt(Minecraft minecraft, String primary, String fallback) {
        try {
            Object window = minecraft.getWindow();
            try {
                return ((Number) window.getClass().getMethod(primary).invoke(window)).intValue();
            } catch (Throwable ignored) {
                return ((Number) window.getClass().getMethod(fallback).invoke(window)).intValue();
            }
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static void enableBlend() {
        try {
            Class<?> renderSystem = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
            try { renderSystem.getMethod("enableBlend").invoke(null); } catch (Throwable ignored) { }
            try { renderSystem.getMethod("defaultBlendFunc").invoke(null); } catch (Throwable ignored) { }
            try { renderSystem.getMethod("disableDepthTest").invoke(null); } catch (Throwable ignored) { }
        } catch (Throwable ignored) {
        }
    }

    private static boolean fill(Object drawContext, int x1, int y1, int x2, int y2, int argb) {
        if (y2 <= y1 || x2 <= x1) return false;

        Class<?> current = drawContext.getClass();
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!"fill".equals(method.getName())) continue;
                Class<?>[] params = method.getParameterTypes();
                try {
                    method.setAccessible(true);
                    if (isFiveIntFill(params)) {
                        method.invoke(drawContext, x1, y1, x2, y2, argb);
                        return true;
                    }
                    if (isPipelineFill(params)) {
                        Object pipeline = getGuiPipeline(params[0]);
                        if (pipeline != null) {
                            method.invoke(drawContext, pipeline, x1, y1, x2, y2, argb);
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

    private static boolean isFiveIntFill(Class<?>[] params) {
        return params.length == 5
                && params[0] == int.class
                && params[1] == int.class
                && params[2] == int.class
                && params[3] == int.class
                && params[4] == int.class;
    }

    private static boolean isPipelineFill(Class<?>[] params) {
        return params.length == 6
                && params[0] != int.class
                && params[1] == int.class
                && params[2] == int.class
                && params[3] == int.class
                && params[4] == int.class
                && params[5] == int.class;
    }

    private static Object getGuiPipeline(Class<?> pipelineClass) {
        if (PIPELINE_CACHE.containsKey(pipelineClass)) return PIPELINE_CACHE.get(pipelineClass);

        Object pipeline = findGuiPipelineIn("net.minecraft.client.renderer.RenderPipelines", pipelineClass);
        if (pipeline == null) pipeline = findGuiPipelineIn("net.minecraft.client.render.RenderPipelines", pipelineClass);
        if (pipeline == null) pipeline = findGuiPipelineIn("com.mojang.blaze3d.pipeline.RenderPipelines", pipelineClass);
        if (pipeline == null) pipeline = findGuiPipelineIn("net.minecraft.client.gui.GuiGraphics", pipelineClass);

        PIPELINE_CACHE.put(pipelineClass, pipeline);
        return pipeline;
    }

    private static Object findGuiPipelineIn(String className, Class<?> pipelineClass) {
        try {
            Class<?> cls = Class.forName(className);
            Object best = null;
            for (Field field : cls.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) continue;
                if (!pipelineClass.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                String name = field.getName().toUpperCase(Locale.ROOT);
                Object value = field.get(null);
                if (value == null) continue;
                if (name.contains("GUI_OVERLAY") || name.equals("GUI")) return value;
                if (best == null && name.contains("GUI")) best = value;
            }
            return best;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void debugStateOccasionally() {
        long now = System.currentTimeMillis();
        if (now - lastStateDebugMs < 5000L) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) return;
        if (isAcidActive(minecraft)) {
            lastStateDebugMs = now;
            System.out.println("[Narcotix] Acid overlay v114 detected active Acid/Nausea.");
        }
    }

    private static int hsvToArgb(float h, float s, float v, int alpha) {
        h = h - (float) Math.floor(h);
        float r = 0.0F;
        float g = 0.0F;
        float b = 0.0F;

        int i = (int) Math.floor(h * 6.0F);
        float f = h * 6.0F - i;
        float p = v * (1.0F - s);
        float q = v * (1.0F - f * s);
        float t = v * (1.0F - (1.0F - f) * s);

        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            case 5 -> { r = v; g = p; b = q; }
            default -> { }
        }

        int ri = Math.max(0, Math.min(255, (int) (r * 255.0F)));
        int gi = Math.max(0, Math.min(255, (int) (g * 255.0F)));
        int bi = Math.max(0, Math.min(255, (int) (b * 255.0F)));
        return ((alpha & 255) << 24) | (ri << 16) | (gi << 8) | bi;
    }
}