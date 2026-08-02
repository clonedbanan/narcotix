package com.example;



import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WeedCropTopBlock extends BushBlock implements BonemealableBlock {
    public static final BooleanProperty BLOOMING = BooleanProperty.create("blooming");
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    public WeedCropTopBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BLOOMING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BLOOMING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(NarcotixMod.WEED_CROP) && below.getValue(WeedCropBlock.AGE) >= WeedCropBlock.MATURE_AGE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos bottomPos = pos.below();
        BlockState bottomState = level.getBlockState(bottomPos);

        if (bottomState.is(NarcotixMod.WEED_CROP) && bottomState.getBlock() instanceof WeedCropBlock weedCropBlock) {
            return weedCropBlock.harvestIfBlooming(bottomState, level, bottomPos);
        }

        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos bottomPos = pos.below();
        BlockState bottomState = level.getBlockState(bottomPos);

        if (bottomState.is(NarcotixMod.WEED_CROP) && bottomState.getBlock() instanceof WeedCropBlock weedCropBlock) {
            weedCropBlock.dropBreakLoot(level, bottomPos, bottomState, player);
            level.destroyBlock(bottomPos, false);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockState bottomState = level.getBlockState(pos.below());

        if (bottomState.is(NarcotixMod.WEED_CROP) && bottomState.getBlock() instanceof WeedCropBlock weedCropBlock) {
            return weedCropBlock.isValidBonemealTarget(level, pos.below(), bottomState);
        }

        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos bottomPos = pos.below();
        BlockState bottomState = level.getBlockState(bottomPos);

        if (bottomState.is(NarcotixMod.WEED_CROP) && bottomState.getBlock() instanceof WeedCropBlock weedCropBlock) {
            weedCropBlock.performBonemeal(level, random, bottomPos, bottomState);
        }
    }
}