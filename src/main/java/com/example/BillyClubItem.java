package com.example;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BillyClubItem extends Item {
    public BillyClubItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && stack.isDamageableItem()) {
            int nextDamage = stack.getDamageValue() + 1;
            if (nextDamage >= stack.getMaxDamage()) {
                stack.shrink(1);
            } else {
                stack.setDamageValue(nextDamage);
            }
        }
    }
}