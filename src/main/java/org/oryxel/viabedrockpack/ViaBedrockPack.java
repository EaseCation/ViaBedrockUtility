package org.oryxel.viabedrockpack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.oryxel.viabedrockpack.entity.CustomEntity;
import org.oryxel.viabedrockpack.entity.renderer.CustomEntityRenderer;
import org.oryxel.viabedrockpack.entity.renderer.model.CustomEntityModel;
import org.oryxel.viabedrockpack.pluginmessage.BedrockMessageHandler;
import org.oryxel.viabedrockpack.pluginmessage.data.BaseData;
import org.oryxel.viabedrockpack.pluginmessage.data.impl.CustomEntityData;
import org.oryxel.viabedrockpack.pluginmessage.data.impl.SpawnEntityData;
import org.oryxel.viabedrockpack.util.GeometryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ViaBedrockPack implements ModInitializer {
	public static final String MOD_ID = "viabedrockpack";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier CUSTOM_ENTITY_IDENTIFIER = Identifier.of(ViaBedrockPack.MOD_ID, "custom-entity");

	public static final PacketCodec<PacketByteBuf, BaseData> STREAM_CODEC = PacketCodec.of(null, buf -> {
		int type = buf.readInt();
		switch (type) {
			case 1 -> {
				final String name = CustomEntityData.readString(buf);
				final int geometryIndex = buf.readInt();
				final Identifier identifier = Identifier.of("viabedrock", "textures/" + CustomEntityData.readString(buf) + ".png");
				final String geometryRaw = CustomEntityData.readString(buf);
				final JsonObject geometry = JsonParser.parseString(geometryRaw).getAsJsonObject();

				return new CustomEntityData(name, geometryIndex, identifier, geometry);
			}

			case 2 -> {
				final String name = CustomEntityData.readString(buf);
				final UUID uuid = buf.readUuid();
				final int entityId = buf.readInt();
				final double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
				final byte yaw = buf.readByte(), pitch = buf.readByte();

				return new SpawnEntityData(name, uuid, entityId, x, y, z, yaw, pitch);
			}

			default -> throw new RuntimeException("Invalid type: " + type);
		}
    });

	@Override
	public void onInitialize() {
		LOGGER.info("Hello ViaBedrockPack!");

		final EntityType<CustomEntity> type = Registry.register(Registries.ENTITY_TYPE, CUSTOM_ENTITY_IDENTIFIER, EntityType.Builder.create(CustomEntity::new, SpawnGroup.MISC).dimensions(0, 0).build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, CUSTOM_ENTITY_IDENTIFIER)));
		EntityRendererRegistry.register(type, (context) -> new CustomEntityRenderer(context, new CustomEntityModel(new ModelPart(List.of(), Map.of())), Identifier.of("empty")));
		FabricDefaultAttributeRegistry.register(type, CustomEntity.createMobAttributes());

		final BedrockMessageHandler handler = new BedrockMessageHandler();
		PayloadTypeRegistry.playS2C().register(CustomEntityData.ID, STREAM_CODEC);
		ClientPlayNetworking.registerGlobalReceiver(CustomEntityData.ID, (payload, context) -> payload.handle(handler));
	}
}