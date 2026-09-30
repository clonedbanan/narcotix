package com.example.mixin;

import com.example.NarcotixMod;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$IngredientsSlot")
public abstract class BrewingStandIngredientsSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void narcotix$allowLsdShard(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.is(NarcotixMod.LSD_SHARD)) {
            cir.setReturnValue(true);
        }
    }
}