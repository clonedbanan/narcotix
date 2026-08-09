package com.example;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

public class WanderingPlugEntity extends AbstractVillager {
    public WanderingPlugEntity(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.7D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.35D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers offers = this.getOffers();
        offers.clear();

        offers.add(sell(NarcotixMod.WEED_SEEDS, 1, 4, 24));
        offers.add(sell(NarcotixMod.COCAINE_SEEDS, 2, 3, 18));
        offers.add(sell(NarcotixMod.TOBACCO_SEEDS, 1, 4, 24));

        offers.add(buy(NarcotixMod.JOINT, 3, 1, 16));
        offers.add(buy(NarcotixMod.BLUNT, 4, 1, 16));
        offers.add(buy(NarcotixMod.CIGARETTE, 2, 1, 20));
        offers.add(buy(NarcotixMod.COKE_BRICK, 12, 1, 8));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!itemStack.isEmpty()) {
            return super.mobInteract(player, hand);
        }

        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        this.updateTrades((ServerLevel) this.level());

        if (!this.getOffers().isEmpty()) {
            this.setTradingPlayer(player);
            this.openTradingScreen(player, this.getDisplayName(), 1);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    public void aiStep() {
    super.aiStep();

    if (!this.level().isClientSide() && this.isTrading()) {
        Player tradingPlayer = this.getTradingPlayer();

        if (tradingPlayer != null) {
            this.getNavigation().stop();
            this.getLookControl().setLookAt(tradingPlayer, 30.0F, 30.0F);
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        }
    }
}

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WANDERING_TRADER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return SoundEvents.WANDERING_TRADER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WANDERING_TRADER_DEATH;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean soldOut) {
        return SoundEvents.WANDERING_TRADER_TRADE;
    }

    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.WANDERING_TRADER_YES;
    }

    private static MerchantOffer sell(Item item, int emeraldCost, int itemCount, int maxUses) {
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCost),
                new ItemStack(item, itemCount),
                maxUses,
                2,
                0.05F
        );
    }

    private static MerchantOffer buy(Item item, int emeraldCount, int itemCount, int maxUses) {
        return new MerchantOffer(
                new ItemCost(item, itemCount),
                new ItemStack(Items.EMERALD, emeraldCount),
                maxUses,
                2,
                0.05F
        );
    }
}
