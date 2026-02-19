package org.oryxel.viabedrockutility.mixin.impl.sound;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    /**
     * Removes the [0.5, 2.0] pitch clamp to support Bedrock Edition's
     * extended pitch range (0.0-256.0).
     * OpenAL natively supports arbitrary positive pitch values.
     */
    @Redirect(
            method = "getAdjustedPitch",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F")
    )
    private float removePitchClamp(float value, float min, float max) {
        return Math.max(value, 0.0f);
    }
}
