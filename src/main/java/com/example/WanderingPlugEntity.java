package com.example;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

public class WanderingPlugEntity extends WanderingTrader {
    public WanderingPlugEntity(EntityType<? extends WanderingTrader> entityType, Level level) {
        super(entityType, level);
        this.setDespawnDelay(24000);
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers offers = this.getOffers();

        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 2),
                new ItemStack(NarcotixMod.WEED_SEEDS, 3),
                16,
                2,
                0.05F
        ));

        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 3),
                new ItemStack(NarcotixMod.TOBACCO_SEEDS, 3),
                16,
                2,
                0.05F
        ));

        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 3),
                new ItemStack(NarcotixMod.COCAINE_SEEDS, 3),
                16,
                2,
                0.05F
        ));


        offers.add(new MerchantOffer(
                new ItemCost(NarcotixMod.JOINT, 2),
                new ItemStack(Items.EMERALD, 1),
                12,
                3,
                0.05F
        ));

        offers.add(new MerchantOffer(
                new ItemCost(NarcotixMod.BLUNT, 2),
                new ItemStack(Items.EMERALD, 1),
                12,
                3,
                0.05F
        ));

        offers.add(new MerchantOffer(
                new ItemCost(NarcotixMod.CIGARETTE, 4),
                new ItemStack(Items.EMERALD, 1),
                12,
                3,
                0.05F
        ));

        offers.add(new MerchantOffer(
                new ItemCost(NarcotixMod.COKE_BRICK, 1),
                new ItemStack(Items.EMERALD, 8),
                8,
                5,
                0.05F
        ));
    }
}
