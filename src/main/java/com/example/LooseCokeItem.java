package com.example;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public class LooseCokeItem extends Item {
    public static final int USE_DURATION = 32;
    public static final int SPEED_DURATION_TICKS = 20 * 15;
    public static final int SPEED_AMPLIFIER = 9;

    public LooseCokeItem(Properties properties) {
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
        return ItemUseAnimation.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            level.playSound(
                    null,
                    entity.getX(), entity.getY(), entity.getZ(),
                    NarcotixMod.LOOSE_COKE_SNIFF_SOUND,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );

            entity.addEffect(new MobEffectInstance(MobEffects.SPEED, SPEED_DURATION_TICKS, SPEED_AMPLIFIER));
            entity.addEffect(new MobEffectInstance(NarcotixEffects.COKE_RUSH, SPEED_DURATION_TICKS, 0));

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.POOF,
                        entity.getX(), entity.getY() + 1.45D, entity.getZ(),
                        8,
                        0.15D, 0.10D, 0.15D,
                        0.01D
                );
            }

            if (entity instanceof Player player && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return stack;
    }
}