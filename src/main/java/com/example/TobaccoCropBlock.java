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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TobaccoCropBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 4);

    public static final int BLOOMING_AGE = 4;

    private static final VoxelShape[] SHAPES = new VoxelShape[] {
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 3.0D, 11.0D),
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 6.0D, 12.0D),
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D),
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 14.0D, 14.0D),
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D)
    };

    public TobaccoCropBlock(BlockBehaviour.Properties properties) {
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
        if (state.getValue(AGE) < BLOOMING_AGE && random.nextInt(4) == 0) {
            growOneStage(level, pos, state);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return harvestIfBlooming(state, level, pos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropBreakLoot(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    public InteractionResult harvestIfBlooming(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(AGE) != BLOOMING_AGE) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            RandomSource random = level.getRandom();
            int leaves = 3 + random.nextInt(3);

            popResource(level, pos, new ItemStack(NarcotixAdditions.TOBACCO_LEAF, leaves));

            if (random.nextFloat() < 0.35F) {
                popResource(level, pos, new ItemStack(NarcotixAdditions.TOBACCO_SEEDS, 1 + random.nextInt(2)));
            }

            level.setBlock(pos, state.setValue(AGE, 3), 2);
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

        popResource(level, pos, new ItemStack(NarcotixAdditions.TOBACCO_SEEDS, 1));

        if (state.getValue(AGE) == BLOOMING_AGE) {
            RandomSource random = level.getRandom();
            int leaves = 3 + random.nextInt(3);
            popResource(level, pos, new ItemStack(NarcotixAdditions.TOBACCO_LEAF, leaves));
        }
    }

    private void growOneStage(ServerLevel level, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);

        if (age < BLOOMING_AGE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
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
    }
}
