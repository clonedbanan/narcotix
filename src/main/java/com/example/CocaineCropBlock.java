package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CocaineCropBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 5);

    public static final int MATURE_AGE = 4;
    public static final int BLOOMING_AGE = 5;

    private static final VoxelShape[] SHAPES = new VoxelShape[] {
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 3.0D, 11.0D),
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 5.0D, 12.0D),
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 9.0D, 13.0D),
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 15.0D, 14.0D),
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D),
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D)
    };

    public CocaineCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.FARMLAND);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(Blocks.FARMLAND);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < BLOOMING_AGE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);

        if (age >= BLOOMING_AGE) {
            ensureTopBlock(level, pos, age);
            return;
        }

        if (random.nextInt(4) == 0) {
            growOneStage(level, pos, state);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        ensureTopBlock(level, pos, state.getValue(AGE));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return harvestIfBlooming(state, level, pos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropBreakLoot(level, pos, state, player);

        BlockPos topPos = pos.above();
        if (level.getBlockState(topPos).is(NarcotixMod.COCAINE_CROP_TOP)) {
            level.removeBlock(topPos, false);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        int age = state.getValue(AGE);

        if (age >= MATURE_AGE && !level.getBlockState(pos.above()).is(NarcotixMod.COCAINE_CROP_TOP)) {
            level.destroyBlock(pos, false);
        }
    }

    public InteractionResult harvestIfBlooming(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(AGE) != BLOOMING_AGE) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            RandomSource random = level.getRandom();
            int leaves = 3 + random.nextInt(3);

            popResource(level, pos, new ItemStack(NarcotixMod.COKE_LEAF, leaves));

            if (random.nextFloat() < 0.35F) {
                popResource(level, pos, new ItemStack(NarcotixMod.COCAINE_SEEDS, 1 + random.nextInt(2)));
            }

            int resetAge = 3;
            level.setBlock(pos, state.setValue(AGE, resetAge), 2);
            ensureTopBlock(level, pos, resetAge);
        }

        return InteractionResult.SUCCESS;
    }

    public void dropBreakLoot(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide()) {
            return;
        }

        if (player.getAbilities().instabuild) {
            return;
        }

        popResource(level, pos, new ItemStack(NarcotixMod.COCAINE_SEEDS, 1));

        if (state.getValue(AGE) == BLOOMING_AGE) {
            RandomSource random = level.getRandom();
            int leaves = 3 + random.nextInt(3);

            popResource(level, pos, new ItemStack(NarcotixMod.COKE_LEAF, leaves));
        }
    }

    private void growOneStage(ServerLevel level, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);

        if (age < BLOOMING_AGE) {
            int newAge = age + 1;
            level.setBlock(pos, state.setValue(AGE, newAge), 2);
            ensureTopBlock(level, pos, newAge);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < BLOOMING_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        int growth = 1 + random.nextInt(2);
        int newAge = Math.min(BLOOMING_AGE, age + growth);

        level.setBlock(pos, state.setValue(AGE, newAge), 2);
        ensureTopBlock(level, pos, newAge);
    }

    public static void ensureTopBlock(Level level, BlockPos bottomPos, int age) {
        BlockPos topPos = bottomPos.above();

        if (age >= MATURE_AGE) {
            boolean blooming = age >= BLOOMING_AGE;
            BlockState desiredTop = NarcotixMod.COCAINE_CROP_TOP.defaultBlockState()
                    .setValue(CocaineCropTopBlock.BLOOMING, blooming);

            BlockState currentTop = level.getBlockState(topPos);
            if (currentTop.isAir() || currentTop.is(NarcotixMod.COCAINE_CROP_TOP)) {
                level.setBlock(topPos, desiredTop, 2);
            }
        } else {
            if (level.getBlockState(topPos).is(NarcotixMod.COCAINE_CROP_TOP)) {
                level.removeBlock(topPos, false);
            }
        }
    }
}
