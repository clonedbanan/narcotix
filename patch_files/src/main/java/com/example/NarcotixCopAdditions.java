package com.example;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.function.Function;

public class NarcotixCopAdditions {
    public static final EntityType<CopEntity> COP = registerEntityType(
            "cop",
            key -> EntityType.Builder.of(CopEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .build(key)
    );

    public static final Item BILLY_CLUB = registerItem(
            "billy_club",
            properties -> new Item(properties.stacksTo(1))
    );

    public static final Item COP_SPAWN_EGG = registerItem(
            "cop_spawn_egg",
            properties -> new NarcotixSpawnEggItem(() -> COP, properties.stacksTo(64))
    );

    public static final Item WANDERING_PLUG_SPAWN_EGG = registerItem(
            "wandering_plug_spawn_egg",
            properties -> new NarcotixSpawnEggItem(() -> NarcotixEntities.WANDERING_PLUG, properties.stacksTo(64))
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(COP, CopEntity.createAttributes());

        SpawnPlacements.register(
                COP,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NarcotixCopAdditions::checkCopSpawnRules
        );

        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                MobCategory.CREATURE,
                COP,
                12,
                1,
                2
        );
    }

    private static boolean checkCopSpawnRules(EntityType<CopEntity> entityType, ServerLevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.below()).isEmpty();
    }

    private static EntityType<CopEntity> registerEntityType(String name, Function<ResourceKey<EntityType<?>>, EntityType<CopEntity>> factory) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(NarcotixMod.MOD_ID, name)
        );

        EntityType<CopEntity> entityType = factory.apply(key);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, key, entityType);
        return entityType;
    }

    private static Item registerItem(String name, Function<Item.Properties, Item> itemFactory) {
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(NarcotixMod.MOD_ID, name)
        );

        Item item = itemFactory.apply(new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }
}
