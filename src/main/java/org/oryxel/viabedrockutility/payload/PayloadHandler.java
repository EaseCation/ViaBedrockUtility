package org.oryxel.viabedrockutility.payload;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
//? if >=1.21.9 {
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
//?} else {
/*import net.minecraft.client.util.SkinTextures;
*///?}
import net.minecraft.util.Identifier;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.oryxel.viabedrockutility.ViaBedrockUtility;
import org.oryxel.viabedrockutility.entity.CustomEntityTicker;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.mixin.impl.accessor.PlayerSkinFieldAccessor;
import org.oryxel.viabedrockutility.pack.PackManager;
import org.oryxel.viabedrockutility.payload.handler.CustomEntityPayloadHandler;
import org.oryxel.viabedrockutility.payload.impl.entity.ModelRequestPayload;
import org.oryxel.viabedrockutility.payload.impl.skin.BaseSkinPayload;
import org.oryxel.viabedrockutility.payload.impl.skin.CapeDataPayload;
import org.oryxel.viabedrockutility.payload.impl.skin.SkinDataPayload;
import org.oryxel.viabedrockutility.renderer.CustomPlayerRenderer;
import org.oryxel.viabedrockutility.util.GeometryUtil;

import org.oryxel.viabedrockutility.util.ImageUtil;
import org.oryxel.viabedrockutility.util.PlayerSkinBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class PayloadHandler {
    protected final Map<UUID, CustomEntityTicker> cachedCustomEntities = new ConcurrentHashMap<>();
    protected final Map<UUID, EntityRenderer<?, ?>> cachedPlayerRenderers = new ConcurrentHashMap<>();
    protected final Map<UUID, Identifier> cachedPlayerCapes = new ConcurrentHashMap<>();
    protected final Map<UUID, SkinInfo> cachedSkinInfo = new ConcurrentHashMap<>();
    protected final Map<UUID, CachedPlayerSkin> cachedPlayerSkins = new ConcurrentHashMap<>();
    protected PackManager packManager;

    public void handle(final BasePayload payload) {
        if (this.packManager != ViaBedrockUtility.getInstance().getPackManager()) {
            this.packManager = ViaBedrockUtility.getInstance().getPackManager();
        }

        if (this.packManager == null) {
            ViaBedrockUtilityFabric.LOGGER.warn("[Payload] Received {} but PackManager is null, ignoring", payload.getClass().getSimpleName());
            return;
        }

        if (payload instanceof ModelRequestPayload modelRequest) {
            this.handle(modelRequest);
        } else if (payload instanceof BaseSkinPayload baseSkin) {
            ViaBedrockUtilityFabric.LOGGER.info("[Skin] Received skin info for player {} ({}x{}, {} chunk(s))", baseSkin.getPlayerUuid(), baseSkin.getSkinWidth(), baseSkin.getSkinHeight(), baseSkin.getChunkCount());
            this.cachedSkinInfo.put(baseSkin.getPlayerUuid(), new SkinInfo(baseSkin.getGeometry(), baseSkin.getResourcePatch(), baseSkin.getSkinWidth(), baseSkin.getSkinHeight(), baseSkin.getChunkCount()));
        } else if (payload instanceof SkinDataPayload skinData) {
            this.handle(skinData);
        } else if (payload instanceof CapeDataPayload capePayload) {
            ViaBedrockUtilityFabric.LOGGER.info("[Skin] Received cape data for player {}", capePayload.getPlayerUuid());
            this.handle(capePayload);
        }
    }

    public void handle(final ModelRequestPayload payload) {}

    public void handle(final CapeDataPayload payload) {
        final NativeImage capeImage = ImageUtil.toNativeImage(payload.getCapeData(), payload.getWidth(), payload.getHeight());
        if (capeImage == null) {
            return;
        }

        final MinecraftClient client = MinecraftClient.getInstance();
        client.getTextureManager().registerTexture(payload.getIdentifier(), new NativeImageBackedTexture(() -> payload.getIdentifier().toString() + capeImage.hashCode() , capeImage));

        if (client.getNetworkHandler() == null) {
            return;
        }

        this.cachedPlayerCapes.put(payload.getPlayerUuid(), payload.getIdentifier());

        // It's ok to use this here, the reason we don't use this for player geometry because there can be fake entity.
        // But most fake entity don't have cape so we should be fine!
        final PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(payload.getPlayerUuid());
        if (entry == null) {
            return;
        }

        final PlayerSkinBuilder builder = new PlayerSkinBuilder(entry.getSkinTextures());
        //? if >=1.21.9 {
        builder.cape = new AssetInfo.TextureAssetInfo(payload.getIdentifier(), payload.getIdentifier());
        //?} else {
        /*builder.capeTexture = payload.getIdentifier();
        *///?}

        ((PlayerSkinFieldAccessor)entry).setPlayerSkin(builder::build);
    }

    private static final List<String> HARDCODED_GEOMETRY_IDENTIFIERS = List.of(
            "geometry.humanoid.custom", "geometry.humanoid.customSlim");

    public void handle(final SkinDataPayload payload) {
        final SkinInfo info = this.cachedSkinInfo.get(payload.getPlayerUuid());
        if (info == null) {
            ViaBedrockUtilityFabric.LOGGER.error("Skin info was null!");
            return;
        }

        info.setData(payload.getSkinData(), payload.getChunkPosition());
        ViaBedrockUtilityFabric.LOGGER.info("Skin chunk {} received for {}", payload.getChunkPosition(), payload.getPlayerUuid());

        if (info.isComplete()) {
            // All skin data has been received
            this.cachedSkinInfo.remove(payload.getPlayerUuid());
        } else {
            return;
        }

        final NativeImage skinImage = ImageUtil.toNativeImage(info.getData(), info.getWidth(), info.getHeight());
        if (skinImage == null) {
            ViaBedrockUtilityFabric.LOGGER.error("[Skin] toNativeImage returned null for {}", payload.getPlayerUuid());
            return;
        }
        ViaBedrockUtilityFabric.LOGGER.info("[Skin] NativeImage created for {} ({}x{})", payload.getPlayerUuid(), info.getWidth(), info.getHeight());

        final MinecraftClient client = MinecraftClient.getInstance();

        final Identifier identifier = Identifier.of(ViaBedrockUtilityFabric.MOD_ID, payload.getPlayerUuid().toString());
        client.getTextureManager().registerTexture(identifier, new NativeImageBackedTexture(() -> identifier.toString() + skinImage.hashCode(), skinImage));
        ViaBedrockUtilityFabric.LOGGER.info("[Skin] Texture registered: {}", identifier);

        if (client.getNetworkHandler() != null) {
            final PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(payload.getPlayerUuid());
            ViaBedrockUtilityFabric.LOGGER.info("[Skin] PlayerListEntry lookup for {}: {}", payload.getPlayerUuid(), entry != null ? entry.getProfile().name() : "NOT FOUND");

            // If we can still get player list entry then use this to set skin still a good idea!
            if (entry != null) {
                final PlayerSkinBuilder builder = new PlayerSkinBuilder(entry.getSkinTextures());
                //? if >=1.21.9 {
                builder.body = new AssetInfo.TextureAssetInfo(identifier, identifier);
                //?} else {
                /*builder.texture = identifier;
                *///?}

                ((PlayerSkinFieldAccessor)entry).setPlayerSkin(builder::build);
            }
        } else {
            ViaBedrockUtilityFabric.LOGGER.warn("[Skin] NetworkHandler is null!");
        }

        // Ex: skinResourcePatch={"geometry":{"default":"geometry.humanoid.custom.1742391406.1704"}}
        String requiredGeometry = null;
        try {
            requiredGeometry = JsonParser.parseString(info.getResourcePatch()).getAsJsonObject()
                    .getAsJsonObject("geometry").get("default").getAsString();
        } catch (Exception ignored) {}
        ViaBedrockUtilityFabric.LOGGER.info("[Skin] requiredGeometry={} resourcePatch={}", requiredGeometry, info.getResourcePatch());

        // Hardcoded I know...
        boolean slim = requiredGeometry != null && requiredGeometry.startsWith("geometry.humanoid.customSlim");

        PlayerEntityModel model = null;
        if (!info.getGeometryRaw().isEmpty()) {
            final List<BedrockGeometryModel> geometries;
            try {
                final JsonObject object = JsonParser.parseString(info.getGeometryRaw()).getAsJsonObject();
                geometries = BedrockGeometryModel.fromJson(object);

                if (!geometries.isEmpty()) {
                    BedrockGeometryModel geometry = geometries.getFirst();
                    if (requiredGeometry != null) {
                        for (final BedrockGeometryModel geometryModel : geometries) {
                            if (geometryModel.getIdentifier().equals(requiredGeometry)) {
                                geometry = geometryModel;
                                break;
                            }
                        }
                    }

                    model = (PlayerEntityModel) GeometryUtil.buildModel(geometry, true, slim);
                    ViaBedrockUtilityFabric.LOGGER.info("[Skin] Built custom geometry model for {}", payload.getPlayerUuid());
                }
            } catch (final Exception e) {
                ViaBedrockUtilityFabric.LOGGER.error("[Skin] Failed to parse geometry for {}: {}", payload.getPlayerUuid(), e.getMessage());
            }
        }

        if (model == null) {
            if (requiredGeometry == null) {
                ViaBedrockUtilityFabric.LOGGER.warn("[Skin] requiredGeometry is null, returning early for {}", payload.getPlayerUuid());
                return;
            }

            boolean found = false;

            for (final String i : HARDCODED_GEOMETRY_IDENTIFIERS) {
                if (i.equals(requiredGeometry) || requiredGeometry.startsWith(i + ".")) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                ViaBedrockUtilityFabric.LOGGER.warn("[Skin] Geometry '{}' not in hardcoded list, returning early for {}", requiredGeometry, payload.getPlayerUuid());
                return;
            }
        }

        if (model == null) {
            // This is likely a classic skin with hardcoded identifier! TODO: 128x128
            model = new PlayerEntityModel(PlayerEntityModel.getTexturedModelData(Dilation.NONE, slim).getRoot().createPart(64, 64), slim);
            ViaBedrockUtilityFabric.LOGGER.info("[Skin] Using default player model (slim={}) for {}", slim, payload.getPlayerUuid());
        }

        //? if >=1.21.9 {
        final EntityRendererFactory.Context entityContext = new EntityRendererFactory.Context(client.getEntityRenderDispatcher(),
                client.getItemModelManager(), client.getMapRenderer(), client.getBlockRenderManager(),
                client.getResourceManager(), client.getLoadedEntityModels(), new EquipmentModelLoader(),
                client.getAtlasManager(), client.textRenderer, client.getPlayerSkinCache());
        //?} else {
        /*final EntityRendererFactory.Context entityContext = new EntityRendererFactory.Context(client.getEntityRenderDispatcher(),
                client.getItemModelManager(), client.getMapRenderer(), client.getBlockRenderManager(),
                client.getResourceManager(), client.getLoadedEntityModels(), new EquipmentModelLoader(),
                client.textRenderer);
        *///?}
        this.cachedPlayerRenderers.put(payload.getPlayerUuid(), new CustomPlayerRenderer(entityContext, model, slim, identifier));
        this.cachedPlayerSkins.put(payload.getPlayerUuid(), new CachedPlayerSkin(identifier, slim));
        ViaBedrockUtilityFabric.LOGGER.info("[Skin] CustomPlayerRenderer created for {}", payload.getPlayerUuid());

        if (client.getNetworkHandler() == null) {
            return;
        }

        final PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(payload.getPlayerUuid());

        // Do this once again for emmmm the slim or wide model.
        if (entry != null) {
            final PlayerSkinBuilder builder = new PlayerSkinBuilder(entry.getSkinTextures());
            //? if >=1.21.9 {
            builder.body = new AssetInfo.TextureAssetInfo(identifier, identifier);
            builder.model = slim ? PlayerSkinType.SLIM : PlayerSkinType.WIDE;
            //?} else {
            /*builder.texture = identifier;
            builder.model = slim ? SkinTextures.Model.SLIM : SkinTextures.Model.WIDE;
            *///?}

            ((PlayerSkinFieldAccessor)entry).setPlayerSkin(builder::build);
            ViaBedrockUtilityFabric.LOGGER.info("[Skin] Final skin applied to PlayerListEntry for {}", payload.getPlayerUuid());
        } else {
            ViaBedrockUtilityFabric.LOGGER.warn("[Skin] Final PlayerListEntry NOT FOUND for {}", payload.getPlayerUuid());
        }
    }

    @Getter
    public static class SkinInfo {
        private final String geometryRaw, resourcePatch;
        private final int width, height;
        private final byte[][] skinData;

        public SkinInfo(String geometryRaw, String resourcePatch, int width, int height, int chunkCount) {
            this.geometryRaw = geometryRaw;
            this.resourcePatch = resourcePatch;
            this.skinData = new byte[chunkCount][];
            this.width = width;
            this.height = height;
        }
        /**
         * Should the skin data be sent to us through multiple plugin messages, assemble it.
         */
        public byte[] getData() {
            if (skinData.length == 1) {
                // No concatenation needed
                return skinData[0];
            }

            int totalLength = 0;
            for (byte[] data : skinData) {
                totalLength += data.length;
            }
            byte[] totalData = new byte[totalLength];
            int currentIndex = 0;
            for (byte[] currentData : skinData) {
                // Copy all arrays to one array
                System.arraycopy(currentData, 0, totalData, currentIndex, currentData.length);
                currentIndex += currentData.length;
            }
            return totalData;
        }

        public void setData(byte[] data, int chunk) {
            this.skinData[chunk] = data;
        }

        public boolean isComplete() {
            for (byte[] data : skinData) {
                if (data == null) {
                    return false;
                }
            }
            return true;
        }
    }

    public void removePlayerCache(UUID uuid) {
        cachedPlayerRenderers.remove(uuid);
        cachedPlayerCapes.remove(uuid);
        cachedPlayerSkins.remove(uuid);
        cachedSkinInfo.remove(uuid);
    }

    @Getter
    public static class CachedPlayerSkin {
        private final Identifier textureId;
        private final boolean slim;

        public CachedPlayerSkin(Identifier textureId, boolean slim) {
            this.textureId = textureId;
            this.slim = slim;
        }
    }
}
