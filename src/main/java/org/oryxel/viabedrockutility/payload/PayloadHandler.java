package org.oryxel.viabedrockutility.payload;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.oryxel.viabedrockutility.ViaBedrockUtility;
import org.oryxel.viabedrockutility.entity.CustomEntity;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.pack.PackManager;
import org.oryxel.viabedrockutility.payload.impl.SpawnRequestPayload;
import org.oryxel.viabedrockutility.util.GeometryUtil;

import java.util.Locale;

public class PayloadHandler {
    private PackManager packManager;

    public void handle(final BasePayload payload) {
        if (this.packManager == null) {
            this.packManager = ViaBedrockUtility.getInstance().getPackManager();
        }

        if (this.packManager == null) {
            return;
        }

        if (payload instanceof SpawnRequestPayload spawnRequest) {
            this.handle(spawnRequest);
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

        entity.model = GeometryUtil.buildCustomModel(geometry);
        client.world.addEntity(entity);
    }

}
