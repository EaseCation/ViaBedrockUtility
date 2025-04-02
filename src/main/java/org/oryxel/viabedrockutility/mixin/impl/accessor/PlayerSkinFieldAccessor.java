package org.oryxel.viabedrockutility.mixin.impl.accessor;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(PlayerListEntry.class)
public interface PlayerSkinFieldAccessor {
    @Accessor("texturesSupplier")
    @Mutable
    void setPlayerSkin(Supplier<SkinTextures> playerSkin);
}
