package org.oryxel.viabedrockutility.mixin.impl.entity;

//? if >=1.21.9 {
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InteractionEntity.class)
public abstract class InteractionEntityMixin extends Entity {
    @Unique
    private PositionInterpolator vbu$interpolator;

    protected InteractionEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public PositionInterpolator getInterpolator() {
        if (this.vbu$interpolator == null) {
            this.vbu$interpolator = new PositionInterpolator(this);
        }
        return this.vbu$interpolator;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void vbu$tickInterpolator(CallbackInfo ci) {
        if (this.vbu$interpolator != null) {
            this.vbu$interpolator.tick();
        }
    }
}
//?} else {
/*
import net.minecraft.entity.decoration.InteractionEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(InteractionEntity.class)
public class InteractionEntityMixin {
    // PositionInterpolator not available before 1.21.9
}
*///?}
