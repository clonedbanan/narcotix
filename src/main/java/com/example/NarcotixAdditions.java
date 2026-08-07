package com.example;

import java.util.function.Function;

import net.fabricmc.api.ModInitializer;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class NarcotixAdditions implements ModInitializer {
    public static final Block TOBACCO_CROP = registerCropBlock(
            "tobacco_crop",
            properties -> new TobaccoCropBlock(properties
                    .noCollision()
                    .randomTicks()
                    .instabreak()
            )
    );

    public static final Item TOBACCO_SEEDS = registerItem(
            "tobacco_seeds",
            properties -> new BlockItem(TOBACCO_CROP, properties)
    );

    public static final Item TOBACCO_LEAF = registerItem(
            "tobacco_leaf",
            Item::new
    );

    public static final Item DRIED_TOBACCO_LEAF = registerItem(
            "dried_tobacco_leaf",
            Item::new
    );

    public static final Item BLUNT = registerItem(
            "blunt",
            JointItem::new
    );

    public static final Item CIGARETTE = registerItem(
            "cigarette",
            JointItem::new
    );

    public static final Block WEED_BLOCK = registerBlockWithItem(
            "weed_block",
            Block::new
    );

    @Override
    public void onInitialize() {
        NarcotixVillageLoot.register();
    }

    private static Item registerItem(String name, Function<Item.Properties, Item> itemFactory) {
        Identifier id = Identifier.fromNamespaceAndPath(NarcotixMod.MOD_ID, name);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        Item item = itemFactory.apply(new Item.Properties().setId(itemKey));

        return Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                item
        );
    }

    private static Block registerCropBlock(String name, Function<BlockBehaviour.Properties, Block> blockFactory) {
        Identifier id = Identifier.fromNamespaceAndPath(NarcotixMod.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);

        Block block = blockFactory.apply(
                BlockBehaviour.Properties.of()
                        .setId(blockKey)
        );

        return Registry.register(
                BuiltInRegistries.BLOCK,
                blockKey,
                block
        );
    }

    private static Block registerBlockWithItem(String name, Function<BlockBehaviour.Properties, Block> blockFactory) {
        Identifier blockId = Identifier.fromNamespaceAndPath(NarcotixMod.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, blockId);

        Block block = blockFactory.apply(
                BlockBehaviour.Properties.of()
                        .strength(0.8F)
                        .setId(blockKey)
        );

        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        Identifier itemId = Identifier.fromNamespaceAndPath(NarcotixMod.MOD_ID, name);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, itemId);

        BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        return block;
    }
}
