package com.example;

import java.util.Set;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class NarcotixVillageLoot {
    private static boolean registered = false;

    private static final Set<ResourceKey<LootTable>> VILLAGE_LOOT_TABLES = Set.of(
            villageChest("village_armorer"),
            villageChest("village_butcher"),
            villageChest("village_cartographer"),
            villageChest("village_desert_house"),
            villageChest("village_fisher"),
            villageChest("village_fletcher"),
            villageChest("village_mason"),
            villageChest("village_plains_house"),
            villageChest("village_savanna_house"),
            villageChest("village_shepherd"),
            villageChest("village_snowy_house"),
            villageChest("village_taiga_house"),
            villageChest("village_tannery"),
            villageChest("village_temple"),
            villageChest("village_toolsmith"),
            villageChest("village_weaponsmith")
    );

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) {
                return;
            }

            if (!VILLAGE_LOOT_TABLES.contains(key)) {
                return;
            }

            tableBuilder.withPool(seedPool(NarcotixMod.WEED_SEEDS, 2, 1.0F, 3.0F));
            tableBuilder.withPool(seedPool(NarcotixMod.COCAINE_SEEDS, 2, 1.0F, 3.0F));
            tableBuilder.withPool(seedPool(NarcotixMod.TOBACCO_SEEDS, 8, 1.0F, 3.0F));
        });
    }

    private static LootPool.Builder seedPool(Item item, int weight, float minCount, float maxCount) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(
                        LootItem.lootTableItem(item)
                                .setWeight(weight)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(minCount, maxCount)))
                );
    }

    private static ResourceKey<LootTable> villageChest(String name) {
        return ResourceKey.create(
                Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath("minecraft", "chests/village/" + name)
        );
    }
}
