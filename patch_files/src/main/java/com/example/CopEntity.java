package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CopEntity extends AbstractVillager {
    private int chestScanCooldown = 0;

    public CopEntity(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarcotixCopAdditions.BILLY_CLUB));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.55D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, WanderingPlugEntity.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide()) {
            if (!this.getMainHandItem().is(NarcotixCopAdditions.BILLY_CLUB)) {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarcotixCopAdditions.BILLY_CLUB));
            }

            if (this.chestScanCooldown-- <= 0) {
                this.chestScanCooldown = 40;
                scanNearbyContainers((ServerLevel) this.level());
            }
        }
    }

    private void scanNearbyContainers(ServerLevel level) {
        BlockPos origin = this.blockPosition();
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-6, -2, -6), origin.offset(6, 2, 6))) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof Container container)) {
                continue;
            }

            if (!containerHasContraband(container)) {
                continue;
            }

            double dx = this.getX() - (pos.getX() + 0.5D);
            double dy = this.getY() - (pos.getY() + 0.5D);
            double dz = this.getZ() - (pos.getZ() + 0.5D);
            double distance = (dx * dx) + (dy * dy) + (dz * dz);

            if (distance < closestDistance) {
                closestDistance = distance;
                closest = pos.immutable();
            }
        }

        if (closest == null) {
            return;
        }

        this.getLookControl().setLookAt(closest.getX() + 0.5D, closest.getY() + 0.5D, closest.getZ() + 0.5D);

        if (closestDistance > 4.0D) {
            this.getNavigation().moveTo(closest.getX() + 0.5D, closest.getY(), closest.getZ() + 0.5D, 0.55D);
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(closest);
        if (blockEntity instanceof Container container) {
            confiscateContraband(container);
        }
    }

    private static boolean containerHasContraband(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (isContraband(container.getItem(i))) {
                return true;
            }
        }

        return false;
    }

    private static void confiscateContraband(Container container) {
        boolean changed = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            if (isContraband(container.getItem(i))) {
                container.setItem(i, ItemStack.EMPTY);
                changed = true;
            }
        }

        if (changed) {
            container.setChanged();
        }
    }

    private static boolean isContraband(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();

        if (item == NarcotixMod.WEED_SEEDS
                || item == NarcotixMod.COCAINE_SEEDS
                || item == NarcotixMod.TOBACCO_SEEDS) {
            return false;
        }

        return item == NarcotixMod.TRIMMED_BUD
                || item == NarcotixMod.JOINT
                || item == NarcotixMod.BLUNT
                || item == NarcotixMod.CIGARETTE
                || item == NarcotixMod.COKE_LEAF
                || item == NarcotixMod.LOOSE_COKE
                || item == NarcotixMod.COKE_BRICK
                || item == NarcotixMod.TOBACCO_LEAF
                || item == NarcotixMod.TOBACCO_LEAF_DRY
                || item == NarcotixMod.WEED_BLOCK.asItem();
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        this.getOffers().clear();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
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
    public MerchantOffers getOffers() {
        return new MerchantOffers();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }
}
