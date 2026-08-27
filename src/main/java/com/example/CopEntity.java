package com.example;



import net.minecraft.tags.BlockTags;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CopEntity extends Vindicator {
        private BlockPos cropSearchTarget = null;
    private int cropBreakWarmup = 0;
    private int cropScanCooldown = 0;
private static final int SEARCH_RADIUS = 8;
    private static final int SEARCH_INTERVAL_TICKS = 40;
    private static final int CHEST_OPEN_WAIT_TICKS = 24;
    private static final int CHEST_CLOSE_WAIT_TICKS = 42;

    private BlockPos inspectingContainerPos;
    private int inspectingContainerTicks;
    private boolean confiscatedFromOpenContainer;

    public CopEntity(EntityType<? extends Vindicator> entityType, Level level) {
        super(entityType, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarcotixCopAdditions.BILLY_CLUB));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vindicator.createAttributes();
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

        if (this.level().isClientSide()) {
            return;
        }

        if (!this.getMainHandItem().is(NarcotixCopAdditions.BILLY_CLUB)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarcotixCopAdditions.BILLY_CLUB));
        }

        ServerLevel serverLevel = (ServerLevel) this.level();

        
        if (handleContrabandCropSearch(serverLevel)) {
            return;
        }
if (this.inspectingContainerPos != null) {
            continueInspectingContainer(serverLevel);
            return;
        }

        if (this.tickCount % SEARCH_INTERVAL_TICKS == 0) {
            scanNearbyContainers(serverLevel);
        }
    }

    private void continueInspectingContainer(ServerLevel level) {
        BlockPos pos = this.inspectingContainerPos;

        if (pos == null || !(level.getBlockEntity(pos) instanceof Container container) || !hasContraband(container)) {
            closeContainerAnimation(level, pos);
            stopInspectingContainer();
            return;
        }

        this.getNavigation().stop();
        this.getLookControl().setLookAt(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));

        this.inspectingContainerTicks++;

        if (this.inspectingContainerTicks == 1) {
            openContainerAnimation(level, pos);
        }

        if (!this.confiscatedFromOpenContainer && this.inspectingContainerTicks >= CHEST_OPEN_WAIT_TICKS) {
            confiscateContraband(container);
            this.confiscatedFromOpenContainer = true;
        }

        if (this.inspectingContainerTicks >= CHEST_CLOSE_WAIT_TICKS) {
            closeContainerAnimation(level, pos);
            stopInspectingContainer();
        }
    }

    private void scanNearbyContainers(ServerLevel level) {
        BlockPos origin = this.blockPosition();
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-SEARCH_RADIUS, -3, -SEARCH_RADIUS), origin.offset(SEARCH_RADIUS, 3, SEARCH_RADIUS))) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (!(blockEntity instanceof Container container) || !hasContraband(container)) {
                continue;
            }

            double dx = this.getX() - (pos.getX() + 0.5D);
            double dy = this.getY() - (pos.getY() + 0.5D);
            double dz = this.getZ() - (pos.getZ() + 0.5D);
            double distance = dx * dx + dy * dy + dz * dz;

            if (distance < closestDistance) {
                closestDistance = distance;
                closest = pos.immutable();
            }
        }

        if (closest == null) {
            return;
        }

        this.getLookControl().setLookAt(closest.getX() + 0.5D, closest.getY() + 0.5D, closest.getZ() + 0.5D);

        if (closestDistance > 6.25D) {
            this.getNavigation().moveTo(closest.getX() + 0.5D, closest.getY(), closest.getZ() + 0.5D, 0.55D);
            return;
        }

        startInspectingContainer(closest);
    }

    private void startInspectingContainer(BlockPos pos) {
        this.inspectingContainerPos = pos.immutable();
        this.inspectingContainerTicks = 0;
        this.confiscatedFromOpenContainer = false;
        this.getNavigation().stop();
    }

    private void stopInspectingContainer() {
        this.inspectingContainerPos = null;
        this.inspectingContainerTicks = 0;
        this.confiscatedFromOpenContainer = false;
    }

    private void openContainerAnimation(ServerLevel level, BlockPos pos) {
        setChestOpen(level, pos, true);
    }

    private void closeContainerAnimation(ServerLevel level, BlockPos pos) {
        setChestOpen(level, pos, false);
    }

    private void setChestOpen(ServerLevel level, BlockPos pos, boolean open) {
        if (pos == null) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof ChestBlockEntity)) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
    }

    private static boolean hasContraband(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (isContraband(container.getItem(i))) {
                return true;
            }
        }

        return false;
    }

    private static void confiscateContraband(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);

            if (isContraband(stack)) {
                container.setItem(i, ItemStack.EMPTY);
            }
        }

        container.setChanged();
    }

    private static boolean isContraband(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.is(NarcotixMod.TRIMMED_BUD)
                || stack.is(NarcotixMod.JOINT)
                || stack.is(NarcotixMod.BLUNT)
                || stack.is(NarcotixMod.COKE_LEAF)
                || stack.is(NarcotixMod.LOOSE_COKE)
                || stack.is(NarcotixMod.COKE_BRICK)
                || stack.is(NarcotixMod.WEED_BLOCK.asItem());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
    private boolean handleContrabandCropSearch(ServerLevel level) {
        if (this.cropSearchTarget != null) {
            BlockPos basePos = getContrabandCropBase(level, this.cropSearchTarget);

            if (basePos == null) {
                clearCropTarget();
                return false;
            }

            BlockPos standPos = findReachableCropStandPosition(level, basePos);

            if (standPos == null) {
                clearCropTarget();
                this.cropScanCooldown = 25;
                return false;
            }

            this.getLookControl().setLookAt(basePos.getX() + 0.5D, basePos.getY() + 0.5D, basePos.getZ() + 0.5D);

            double dx = this.getX() - (standPos.getX() + 0.5D);
            double dy = this.getY() - standPos.getY();
            double dz = this.getZ() - (standPos.getZ() + 0.5D);
            double distanceSqr = (dx * dx) + (dy * dy) + (dz * dz);

            if (distanceSqr > 1.85D) {
                this.cropBreakWarmup = 0;
                this.getNavigation().moveTo(standPos.getX() + 0.5D, standPos.getY(), standPos.getZ() + 0.5D, 0.7D);
                return true;
            }

            // Do not allow breaking from the wrong side of a wall/fence just because the crop is physically close.
            if (!isActuallyStandingAtCropAccess(level, basePos)) {
                this.cropBreakWarmup = 0;
                this.getNavigation().moveTo(standPos.getX() + 0.5D, standPos.getY(), standPos.getZ() + 0.5D, 0.7D);
                return true;
            }

            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            this.cropBreakWarmup++;

            if (this.cropBreakWarmup >= 22) {
                breakContrabandCrop(level, basePos);
                clearCropTarget();
                this.cropScanCooldown = 10;
            }

            return true;
        }

        if (this.cropScanCooldown > 0) {
            this.cropScanCooldown--;
            return false;
        }

        this.cropScanCooldown = 20;
        this.cropSearchTarget = findNearestContrabandCrop(level);
        return this.cropSearchTarget != null;
    }

    private void clearCropTarget() {
        this.cropSearchTarget = null;
        this.cropBreakWarmup = 0;
    }

    private BlockPos findNearestContrabandCrop(ServerLevel level) {
        BlockPos origin = this.blockPosition();
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (BlockPos scanPos : BlockPos.betweenClosed(origin.offset(-14, -4, -14), origin.offset(14, 4, 14))) {
            BlockPos basePos = getContrabandCropBase(level, scanPos);

            if (basePos == null) {
                continue;
            }

            // Fences, walls, and closed barriers should stop cops from choosing the crop at all.
            if (findReachableCropStandPosition(level, basePos) == null) {
                continue;
            }

            double dx = this.getX() - (basePos.getX() + 0.5D);
            double dy = this.getY() - (basePos.getY() + 0.5D);
            double dz = this.getZ() - (basePos.getZ() + 0.5D);
            double distance = (dx * dx) + (dy * dy) + (dz * dz);

            if (distance < closestDistance) {
                closestDistance = distance;
                closest = basePos.immutable();
            }
        }

        return closest;
    }

    private BlockPos findReachableCropStandPosition(ServerLevel level, BlockPos basePos) {
        BlockPos closestStand = null;
        double closestDistance = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos standPos = basePos.relative(direction);

            if (!canStandAtCropAccess(level, standPos)) {
                continue;
            }

            if (!hasPathToStandPosition(standPos)) {
                continue;
            }

            double dx = this.getX() - (standPos.getX() + 0.5D);
            double dy = this.getY() - standPos.getY();
            double dz = this.getZ() - (standPos.getZ() + 0.5D);
            double distance = (dx * dx) + (dy * dy) + (dz * dz);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestStand = standPos.immutable();
            }
        }

        return closestStand;
    }

    private boolean hasPathToStandPosition(BlockPos standPos) {
        // Already standing there counts. This avoids occasional tiny pathing failures at point-blank range.
        double dx = this.getX() - (standPos.getX() + 0.5D);
        double dy = this.getY() - standPos.getY();
        double dz = this.getZ() - (standPos.getZ() + 0.5D);

        if (((dx * dx) + (dy * dy) + (dz * dz)) <= 1.85D) {
            return true;
        }

        var path = this.getNavigation().createPath(standPos, 0);
        return path != null && path.canReach();
    }

    private boolean isActuallyStandingAtCropAccess(ServerLevel level, BlockPos basePos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos standPos = basePos.relative(direction);

            if (!canStandAtCropAccess(level, standPos)) {
                continue;
            }

            double dx = this.getX() - (standPos.getX() + 0.5D);
            double dy = this.getY() - standPos.getY();
            double dz = this.getZ() - (standPos.getZ() + 0.5D);

            if (((dx * dx) + (dy * dy) + (dz * dz)) <= 1.85D) {
                return true;
            }
        }

        return false;
    }

    private boolean canStandAtCropAccess(ServerLevel level, BlockPos standPos) {
        BlockState feetState = level.getBlockState(standPos);
        BlockState headState = level.getBlockState(standPos.above());
        BlockState floorState = level.getBlockState(standPos.below());

        if (isFenceLike(feetState) || isFenceLike(headState) || isFenceLike(floorState)) {
            return false;
        }

        if (!feetState.getCollisionShape(level, standPos).isEmpty()) {
            return false;
        }

        if (!headState.getCollisionShape(level, standPos.above()).isEmpty()) {
            return false;
        }

        return !floorState.getCollisionShape(level, standPos.below()).isEmpty();
    }

    private boolean isFenceLike(BlockState state) {
        return state.is(BlockTags.FENCES)
                || state.is(BlockTags.FENCE_GATES)
                || state.is(BlockTags.WALLS);
    }

    private BlockPos getContrabandCropBase(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.is(NarcotixMod.WEED_CROP) || state.is(NarcotixMod.COCAINE_CROP)) {
            return pos.immutable();
        }

        if (state.is(NarcotixMod.WEED_CROP_TOP) || state.is(NarcotixMod.COCAINE_CROP_TOP)) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);

            if (belowState.is(NarcotixMod.WEED_CROP) || belowState.is(NarcotixMod.COCAINE_CROP)) {
                return below.immutable();
            }
        }

        return null;
    }

    private void breakContrabandCrop(ServerLevel level, BlockPos basePos) {
        BlockState baseState = level.getBlockState(basePos);

        if (baseState.is(NarcotixMod.WEED_CROP) || baseState.is(NarcotixMod.COCAINE_CROP)) {
            BlockPos topPos = basePos.above();
            BlockState topState = level.getBlockState(topPos);

            if (topState.is(NarcotixMod.WEED_CROP_TOP) || topState.is(NarcotixMod.COCAINE_CROP_TOP)) {
                level.destroyBlock(topPos, false);
            }
        }

        level.destroyBlock(basePos, false);
    }
}