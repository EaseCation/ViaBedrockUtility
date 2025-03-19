package org.oryxel.viabedrockutility.payload.impl.entity;

import lombok.Getter;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import org.oryxel.viabedrockutility.ViaBedrockUtility;
import org.oryxel.viabedrockutility.payload.BasePayload;

import java.util.UUID;

@Getter
public class SpawnRequestPayload extends BasePayload {
    private final EntityType<?> entityType;
    private final String geometry, texture;
    private final int entityId;
    private final UUID uuid;
    private final double x, y, z;
    private final byte pitch, yaw, headYaw;

    public SpawnRequestPayload(final String geometry, final String texture, final int entityId, final UUID uuid, final double x, final double y, final double z, final byte pitch, final byte yaw, final byte headYaw) {
        this.entityType = Registries.ENTITY_TYPE.get(ViaBedrockUtility.CUSTOM_ENTITY_IDENTIFIER);
        this.geometry = geometry;
        this.texture = texture;
        this.entityId = entityId;
        this.uuid = uuid;
        this.x = x;
        this.y = y;
        this.z = z;

        this.pitch = pitch;
        this.yaw = yaw;
        this.headYaw = headYaw;
    }
}
