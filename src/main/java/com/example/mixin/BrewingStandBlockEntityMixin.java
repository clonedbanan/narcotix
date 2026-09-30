package com.example.mixin;

import com.example.NarcotixMod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {
    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void narcotix$allowLsdShardInIngredientSlot(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (slot == 3 && stack.is(NarcotixMod.LSD_SHARD)) {
            cir.setReturnValue(true);
        }
    }
}