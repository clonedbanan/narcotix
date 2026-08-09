package com.example;





import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import java.util.function.Function;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;

public final class NarcotixEntities {
    public static final EntityType<WanderingPlugEntity> WANDERING_PLUG = register(
            "wandering_plug",
            key -> EntityType.Builder.of(WanderingPlugEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(key)
    );

    private NarcotixEntities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(WANDERING_PLUG, WanderingTrader.createMobAttributes());

        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                MobCategory.CREATURE,
                WANDERING_PLUG,
                35,
                1,
                1
        );
        SpawnPlacements.register(
                WANDERING_PLUG,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules
        );
    }

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(
            String name,
            Function<ResourceKey<EntityType<?>>, EntityType<T>> factory
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(NarcotixMod.MOD_ID, name);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, factory.apply(key));
    }
}
