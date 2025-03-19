package org.oryxel.viabedrockutility.util;

import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

public final class PlayerSkinBuilder {
    public Identifier texture;
    public String textureUrl;
    public Identifier capeTexture;
    public Identifier elytraTexture;
    public SkinTextures.Model model;
    public boolean secure;

    public PlayerSkinBuilder(final SkinTextures base) {
        this.texture = base.texture();
        this.textureUrl = base.textureUrl();
        this.capeTexture = base.capeTexture();
        this.elytraTexture = base.elytraTexture();
        this.model = base.model();
        this.secure = base.secure();
    }

    public SkinTextures build() {
        return new SkinTextures(texture, textureUrl, capeTexture, elytraTexture, model, secure);
    }
}