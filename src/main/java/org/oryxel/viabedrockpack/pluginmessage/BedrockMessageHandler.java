package org.oryxel.viabedrockpack.pluginmessage;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.registry.Registries;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.oryxel.viabedrockpack.ViaBedrockPack;
import org.oryxel.viabedrockpack.entity.CustomEntity;
import org.oryxel.viabedrockpack.manager.CustomEntityManager;
import org.oryxel.viabedrockpack.pluginmessage.data.BaseData;
import org.oryxel.viabedrockpack.pluginmessage.data.impl.CustomEntityData;
import org.oryxel.viabedrockpack.pluginmessage.data.impl.SpawnEntityData;
import org.oryxel.viabedrockpack.util.GeometryUtil;

import java.util.List;

public class BedrockMessageHandler {
    public void handle(final BaseData data) {
        if (data instanceof CustomEntityData custom) {
            handle(custom);
        } else if (data instanceof SpawnEntityData spawn) {
            handle(spawn);
        }
    }

    public void handle(final SpawnEntityData data) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        final EntityType<?> type = Registries.ENTITY_TYPE.get(ViaBedrockPack.CUSTOM_ENTITY_IDENTIFIER);
        Entity tempEntity = type.create(client.world, SpawnReason.LOAD);
        if (!(tempEntity instanceof CustomEntity entity)) {
            return;
        }

        double d = data.getX();
        double e = data.getY();
        double f = data.getZ();
        float g = data.getYaw();
        float h = data.getPitch();

        entity.updateTrackedPosition(d, e, f);
        entity.prevBodyYaw = entity.prevHeadYaw = entity.bodyYaw = data.getYaw();
        entity.headYaw = data.getYaw();
        entity.setId(data.getEntityId());
        entity.setUuid(data.getUuid());
        entity.updatePositionAndAngles(d, e, f, g, h);

        client.world.addEntity(entity);
    }

    public void handle(final CustomEntityData data) {
        final List<BedrockGeometryModel> models = BedrockGeometryModel.fromJson(data.getGeometryData());
        CustomEntityManager.put(data.getName(), GeometryUtil.buildCustomModel(models.get(data.getGeometryIndex())), data.getTexture());
    }
}
