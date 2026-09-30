package com.example;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class NarcotixEffects {
    public static final Holder<MobEffect> WEED_HIGH = register("weed_high", 0xD7A45A);
    public static final Holder<MobEffect> COKE_RUSH = register("coke_rush", 0xF5F5F5);
    public static final Holder<MobEffect> TOBACCO = register("tobacco", 0xB47A44);
    public static final Holder<MobEffect> ACID = register("acid", 0xB86BFF);

    private NarcotixEffects() {
    }

    public static void register() {
        NarcotixMod.LOGGER.info("Registered Narcotix status effects.");
    }

    private static Holder<MobEffect> register(String name, int color) {
        Identifier id = Identifier.fromNamespaceAndPath(NarcotixMod.MOD_ID, name);
        return Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                id,
                new BasicNarcotixEffect(MobEffectCategory.BENEFICIAL, color)
        );
    }

    private static class BasicNarcotixEffect extends MobEffect {
        protected BasicNarcotixEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}