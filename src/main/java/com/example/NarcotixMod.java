package com.example;

import java.util.function.Function;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import java.util.function.Function;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NarcotixMod implements ModInitializer {
    public static final String MOD_ID = "narcotix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Block WEED_CROP = registerBlockNoItem(
            "weed_crop",
            WeedCropBlock::new,
            BlockBehaviour.Properties.of()
                    .noCollision()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
    );

    public static final Block WEED_CROP_TOP = registerBlockNoItem(
            "weed_crop_top",
            WeedCropTopBlock::new,
            BlockBehaviour.Properties.of()
                    .noCollision()
                    .instabreak()
                    .sound(SoundType.CROP)
    );

    public static final Block COCAINE_CROP = registerCropBlock(
        "cocaine_crop",
        properties -> new CocaineCropBlock(properties
                .noCollision()
                .randomTicks()
                .instabreak()
        )
);

public static final Block COCAINE_CROP_TOP = registerCropBlock(
        "cocaine_crop_top",
        properties -> new CocaineCropTopBlock(properties
                .noCollision()
                .randomTicks()
                .instabreak()
        )
);

private static Block registerCropBlock(String name, Function<BlockBehaviour.Properties, Block> blockFactory) {
    Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, name);
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

public static final Item COCAINE_SEEDS = registerItem(
        "cocaine_seeds",
        properties -> new BlockItem(COCAINE_CROP, properties)
);

public static final Item COKE_LEAF = registerItem(
        "coke_leaf",
        Item::new
);

public static final Item LOOSE_COKE = registerItem(
        "loose_coke",
        Item::new
);

public static final Item COKE_BRICK = registerItem(
        "coke_brick",
        Item::new
);

    public static final Item WEED_SEEDS = registerItem(
            "weed_seeds",
            properties -> new BlockItem(WEED_CROP, properties)
    );

    public static final Item TRIMMED_BUD = registerItem(
            "trimmed_bud",
            Item::new
    );

    public static final Item JOINT = registerItem("joint", properties -> new JointItem(properties));

    @Override
    public void onInitialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
            entries.accept(WEED_SEEDS);
            entries.accept(COCAINE_SEEDS);
            NarcotixVillageLoot.register();
            
        });

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(TRIMMED_BUD);
            entries.accept(JOINT);
            entries.accept(COKE_LEAF);
            entries.accept(LOOSE_COKE);
            entries.accept(COKE_BRICK);
        });

        LOGGER.info("Narcotix loaded.");
    }

   

    private static Block registerBlockNoItem(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        Identifier id = id(name);
        ResourceKey<Block> key = ResourceKey.create(BuiltInRegistries.BLOCK.key(), id);
        Block block = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    private static Item registerItem(String name, Function<Item.Properties, Item> factory) {
        Identifier id = id(name);
        ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        Item item = factory.apply(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
    
}
