package com.example;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemUseAnimation;
public class JointItem extends Item {
    public static final int USE_DURATION = 90;

    public JointItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }


    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
    return ItemUseAnimation.SPYGLASS;
}
    

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    entity.getX(),
                    entity.getY() + 1.35D,
                    entity.getZ(),
                    24,
                    0.3D,
                    0.3D,
                    0.3D,
                    0.015D
            );

            if (entity instanceof Player player && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return stack;
    }
}