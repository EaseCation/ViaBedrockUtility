package org.oryxel.viabedrockutility;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.oryxel.viabedrockutility.entity.CustomEntity;
import org.oryxel.viabedrockutility.entity.renderer.CustomEntityRenderer;
import org.oryxel.viabedrockutility.entity.renderer.model.CustomEntityModel;
import org.oryxel.viabedrockutility.payload.PayloadHandler;
import org.oryxel.viabedrockutility.payload.BasePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ViaBedrockUtility implements ModInitializer {
	public static final String MOD_ID = "viabedrockutility";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier CUSTOM_ENTITY_IDENTIFIER = Identifier.of(ViaBedrockUtility.MOD_ID, "custom-entity");

	@Override
	public void onInitialize() {
		LOGGER.info("Hello World!");

		// Register custom entity.
		final EntityType<CustomEntity> type = Registry.register(Registries.ENTITY_TYPE, CUSTOM_ENTITY_IDENTIFIER, EntityType.Builder.create(CustomEntity::new, SpawnGroup.MISC).dimensions(0, 0).build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, CUSTOM_ENTITY_IDENTIFIER)));
		EntityRendererRegistry.register(type, (context) -> new CustomEntityRenderer(context, new CustomEntityModel(new ModelPart(List.of(), Map.of())), Identifier.of("empty")));
		FabricDefaultAttributeRegistry.register(type, CustomEntity.createMobAttributes());

		// Register custom payload.
		final PayloadHandler handler = new PayloadHandler();
		PayloadTypeRegistry.playS2C().register(BasePayload.ID, BasePayload.STREAM_CODEC);
		ClientPlayNetworking.registerGlobalReceiver(BasePayload.ID, (payload, context) -> payload.handle(handler));
	}
}