package org.oryxel.viabedrockutility.util;

//? if >=1.21.9 {
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
//?} else {
/*import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
*///?}

public final class PlayerSkinBuilder {
    //? if >=1.21.9 {
    public AssetInfo.TextureAsset body;
    public AssetInfo.TextureAsset cape;
    public AssetInfo.TextureAsset elytra;
    public PlayerSkinType model;
    public boolean secure;

    public PlayerSkinBuilder(final SkinTextures base) {
        this.body = base.body();
        this.cape = base.cape();
        this.elytra = base.elytra();
        this.model = base.model();
        this.secure = base.secure();
    }

    public SkinTextures build() {
        return new SkinTextures(body, cape, elytra, model, secure);
    }
    //?} else {
    /*public Identifier texture;
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
    *///?}
}
