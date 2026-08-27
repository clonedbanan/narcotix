package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class NarcotixSpawnEggItem extends Item {
    private final Supplier<? extends EntityType<? extends Entity>> entityTypeSupplier;

    public NarcotixSpawnEggItem(Supplier<? extends EntityType<? extends Entity>> entityTypeSupplier, Properties properties) {
        super(properties);
        this.entityTypeSupplier = entityTypeSupplier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        EntityType<? extends Entity> entityType = this.entityTypeSupplier.get();
        Entity entity;

        if (entityType == NarcotixCopAdditions.COP) {
            entity = new CopEntity(NarcotixCopAdditions.COP, level);
        } else if (entityType == NarcotixEntities.WANDERING_PLUG) {
            entity = new WanderingPlugEntity(NarcotixEntities.WANDERING_PLUG, level);
        } else {
            return InteractionResult.PASS;
        }

        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        level.addFreshEntity(entity);

        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}
