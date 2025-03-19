package org.oryxel.viabedrockutility.payload;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.oryxel.viabedrockutility.ViaBedrockUtility;
import org.oryxel.viabedrockutility.entity.CustomEntity;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.mixin.accessor.PlayerSkinFieldAccessor;
import org.oryxel.viabedrockutility.pack.PackManager;
import org.oryxel.viabedrockutility.payload.impl.entity.SpawnRequestPayload;
import org.oryxel.viabedrockutility.payload.impl.skin.BaseSkinPayload;
import org.oryxel.viabedrockutility.payload.impl.skin.CapeDataPayload;
import org.oryxel.viabedrockutility.payload.impl.skin.SkinDataPayload;
import org.oryxel.viabedrockutility.renderer.CustomPlayerRenderer;
import org.oryxel.viabedrockutility.util.GeometryUtil;

import org.oryxel.viabedrockutility.renderer.model.CustomEntityModel;
import org.oryxel.viabedrockutility.util.ImageUtil;
import org.oryxel.viabedrockutility.util.PlayerSkinBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class PayloadHandler {
    private final Map<String, CustomPlayerRenderer> cachedPlayerRenderers = new ConcurrentHashMap<>();
    private final Map<String, SkinInfo> cachedSkinInfo = new ConcurrentHashMap<>();
    private PackManager packManager;

    public void handle(final BasePayload payload) {
        if (this.packManager != ViaBedrockUtility.getInstance().getPackManager()) {
            this.packManager = ViaBedrockUtility.getInstance().getPackManager();
        }

        if (this.packManager == null) {
            return;
        }

        if (payload instanceof SpawnRequestPayload spawnRequest) {
            this.handle(spawnRequest);
        } else if (payload instanceof BaseSkinPayload baseSkin) {
            this.cachedSkinInfo.put(baseSkin.getPlayerUuid().toString(), new SkinInfo(baseSkin.getGeometry(), baseSkin.getResourcePatch(), baseSkin.getSkinWidth(), baseSkin.getSkinHeight(), baseSkin.getChunkCount()));
        } else if (payload instanceof SkinDataPayload skinData) {
            this.handle(skinData);
        } else if (payload instanceof CapeDataPayload capePayload) {
            this.handle(capePayload);
        }
    }

    public void handle(final SpawnRequestPayload payload) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        final Entity rawEntity = payload.getEntityType().create(client.world, SpawnReason.LOAD);
        if (!(rawEntity instanceof CustomEntity entity)) {
            ViaBedrockUtilityFabric.LOGGER.warn("Failed to spawn entity with geometry={}, texture={}, reason={}", payload.getGeometry(), payload.getTexture(), "Failed to spawn entity!");
            return;
        }

        entity.onSpawnPacket(new EntitySpawnS2CPacket(payload.getEntityId(), payload.getUuid(), payload.getX(), payload.getY(), payload.getZ(), payload.getPitch(), payload.getYaw(), payload.getEntityType(), 0, Vec3d.ZERO, payload.getHeadYaw()));
        entity.texture = Identifier.ofVanilla(payload.getTexture().toLowerCase(Locale.ROOT));

        final BedrockGeometryModel geometry = this.packManager.getModelDefinitions().getEntityModels().get(payload.getGeometry());
        if (geometry == null) {
            ViaBedrockUtilityFabric.LOGGER.warn("Failed to spawn entity with geometry={}, texture={}, reason={}", payload.getGeometry(), payload.getTexture(), "Failed to find geometry!");
            return;
        }

        entity.model = (CustomEntityModel) GeometryUtil.buildModel(geometry, false, false);
        client.world.addEntity(entity);
    }

    public void handle(final CapeDataPayload payload) {
        final NativeImage capeImage = ImageUtil.toNativeImage(payload.getCapeData(), payload.getWidth(), payload.getHeight());
        if (capeImage == null) {
            return;
        }

        final MinecraftClient client = MinecraftClient.getInstance();
        client.getTextureManager().registerTexture(payload.getIdentifier(), new NativeImageBackedTexture(capeImage));

        if (client.getNetworkHandler() == null) {
            return;
        }

        // It's ok to use this here, the reason we don't use this for player geometry because there can be fake entity.
        // But most fake entity don't have cape so we should be fine!
        final PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(payload.getPlayerUuid());
        if (entry == null) {
            return;
        }

        final PlayerSkinBuilder builder = new PlayerSkinBuilder(entry.getSkinTextures());
        builder.capeTexture = payload.getIdentifier();

        ((PlayerSkinFieldAccessor)entry).setPlayerSkin(builder::build);
    }

    public void handle(final SkinDataPayload payload) {
        final SkinInfo info = this.cachedSkinInfo.get(payload.getPlayerUuid().toString());
        if (info == null) {
            ViaBedrockUtilityFabric.LOGGER.error("Skin info was null!");
            return;
        }

        info.setData(payload.getSkinData(), payload.getChunkPosition());
        ViaBedrockUtilityFabric.LOGGER.info("Skin chunk {} received for {}", payload.getChunkPosition(), payload.getPlayerUuid());

        if (info.isComplete()) {
            // All skin data has been received
            this.cachedSkinInfo.remove(payload.getPlayerUuid().toString());
        } else {
            return;
        }

        final NativeImage skinImage = ImageUtil.toNativeImage(info.getData(), info.getWidth(), info.getHeight());
        if (skinImage == null) {
            return;
        }

        final MinecraftClient client = MinecraftClient.getInstance();

        final Identifier identifier = Identifier.of(ViaBedrockUtilityFabric.MOD_ID, payload.getPlayerUuid().toString());
        client.getTextureManager().registerTexture(identifier, new NativeImageBackedTexture(skinImage));

        if (client.getNetworkHandler() != null) {
            final PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(payload.getPlayerUuid());

            // If we can still get player list entry then use this to set skin still a good idea!
            if (entry != null) {
                final PlayerSkinBuilder builder = new PlayerSkinBuilder(entry.getSkinTextures());
                builder.texture = identifier;

                ((PlayerSkinFieldAccessor)entry).setPlayerSkin(builder::build);
            }
        }

        if (info.getGeometryRaw().isEmpty()) {
            return;
        }

        final List<BedrockGeometryModel> geometries;
        try {
            final JsonObject object = JsonParser.parseString(info.getGeometryRaw()).getAsJsonObject();
            geometries = BedrockGeometryModel.fromJson(object);
        } catch (final Exception ignored) {
            return;
        }

        if (geometries.isEmpty()) {
            return;
        }

        final EntityRendererFactory.Context entityContext = new EntityRendererFactory.Context(client.getEntityRenderDispatcher(),
                client.getItemModelManager(), client.getMapRenderer(), client.getBlockRenderManager(),
                client.getResourceManager(), client.getLoadedEntityModels(), new EquipmentModelLoader(), client.textRenderer);

        // Ex: skinResourcePatch={"geometry":{"default":"geometry.humanoid.custom.1742391406.1704"}}
        String requiredGeometry = null;
        try {
            requiredGeometry = JsonParser.parseString(info.getResourcePatch()).getAsJsonObject()
                    .getAsJsonObject("geometry").get("default").getAsString();
        } catch (Exception ignored) {}

        BedrockGeometryModel geometry = geometries.getFirst();
        if (requiredGeometry != null) {
            for (final BedrockGeometryModel geometryModel : geometries) {
                if (geometryModel.getIdentifier().equals(requiredGeometry)) {
                    geometry = geometryModel;
                    requiredGeometry = geometryModel.getIdentifier();
                    break;
                }
            }
        }

        // Hardcoded I know...
        // TODO: Figure this out based off the model, not the identifier.
        boolean slim = requiredGeometry != null && (requiredGeometry.contains("humanoid.customSlim") || requiredGeometry.contains("humanoid.slim"));

        // Convert Bedrock JSON geometry into a class format that Java understands
        final PlayerEntityModel model = (PlayerEntityModel) GeometryUtil.buildModel(geometry, true, slim);
        this.cachedPlayerRenderers.put(payload.getPlayerUuid().toString(), new CustomPlayerRenderer(entityContext, model, slim, identifier));

        if (client.getNetworkHandler() == null) {
            return;
        }

        final PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(payload.getPlayerUuid());

        // Do this once again for emmmm the slim or wide model.
        if (entry != null) {
            final PlayerSkinBuilder builder = new PlayerSkinBuilder(entry.getSkinTextures());
            builder.texture = identifier;
            builder.model = slim ? SkinTextures.Model.SLIM : SkinTextures.Model.WIDE;

            ((PlayerSkinFieldAccessor)entry).setPlayerSkin(builder::build);
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
}
