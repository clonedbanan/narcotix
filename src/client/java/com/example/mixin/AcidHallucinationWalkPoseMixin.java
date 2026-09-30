package com.example.mixin;

import com.example.AcidHallucinationManager;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(HumanoidModel.class)
public class AcidHallucinationWalkPoseMixin {
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL")
    )
    private void narcotix$calmHallucinationWalkPose(HumanoidRenderState state, CallbackInfo ci) {
        if (!isAcidHallucinationState(state)) {
            return;
        }

        /*
         * Fixed, calm render-only walk cycle.
         * This intentionally avoids the adaptive/run intensity that caused spazzy legs.
         */
        float phase = calmPhase(state);
        float swing = (float)Math.sin(phase) * 0.38F;
        float counterSwing = (float)Math.sin(phase + Math.PI) * 0.38F;

        this.rightLeg.xRot = swing;
        this.leftLeg.xRot = counterSwing;
        this.rightArm.xRot = counterSwing * 0.35F;
        this.leftArm.xRot = swing * 0.35F;
    }

    private static float calmPhase(HumanoidRenderState state) {
        int hash = System.identityHashCode(state);
        long time = System.currentTimeMillis();
        return (float)((time / 180.0D) + (hash % 31));
    }

    private static boolean isAcidHallucinationState(Object state) {
        Object found = findHallucinationObject(state, 0);
        return found != null;
    }

    private static Object findHallucinationObject(Object obj, int depth) {
        if (obj == null || depth > 2) {
            return null;
        }

        try {
            if (AcidHallucinationManager.isHallucination(obj)) {
                return obj;
            }
        } catch (Throwable ignored) {
        }

        Class<?> c = obj.getClass();
        while (c != null) {
            Field[] fields;
            try {
                fields = c.getDeclaredFields();
            } catch (Throwable ignored) {
                return null;
            }

            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value == null || value == obj) {
                        continue;
                    }
                    if (AcidHallucinationManager.isHallucination(value)) {
                        return value;
                    }

                    String typeName = value.getClass().getName();
                    if (depth < 2 && (typeName.contains("RenderState") || typeName.contains("Avatar") || typeName.contains("Player"))) {
                        Object nested = findHallucinationObject(value, depth + 1);
                        if (nested != null) {
                            return nested;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            c = c.getSuperclass();
        }

        return null;
    }
}