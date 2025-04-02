package org.oryxel.viabedrockutility.mixin.impl.entity.player;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.oryxel.viabedrockutility.ViaBedrockUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerMixin extends Entity {
    @Shadow private PlayerListEntry playerListEntry;

    public AbstractClientPlayerMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "getSkinTextures", at = @At(value = "TAIL"), cancellable = true)
    public void injectGetSkin(CallbackInfoReturnable<SkinTextures> cir) {
        if (!ViaBedrockUtility.getInstance().isViaBedrockPresent()) {
            return;
        }

        Identifier cape = ViaBedrockUtility.getInstance().getPayloadHandler().getCachedPlayerCapes().get(getUuid());
        if (cape != null) {
            SkinTextures skin = playerListEntry == null ? DefaultSkinHelper.getSkinTextures(this.getUuid()) : playerListEntry.getSkinTextures();
            if (!cape.equals(skin.capeTexture())) {
                cir.setReturnValue(new SkinTextures(
                        skin.texture(),
                        skin.textureUrl(),
                        cape,
                        skin.elytraTexture(),
                        skin.model(),
                        skin.secure()
                ));
            }
        }
    }

}