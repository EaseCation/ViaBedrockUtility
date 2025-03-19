package org.oryxel.viabedrockutility;

import lombok.Getter;
import lombok.Setter;
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
import org.oryxel.viabedrockutility.renderer.CustomEntityRenderer;
import org.oryxel.viabedrockutility.renderer.model.CustomEntityModel;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.pack.PackManager;
import org.oryxel.viabedrockutility.payload.BasePayload;
import org.oryxel.viabedrockutility.payload.PayloadHandler;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ViaBedrockUtility {
    @Getter
    private static final ViaBedrockUtility instance = new ViaBedrockUtility();

    public static final Identifier CUSTOM_ENTITY_IDENTIFIER = Identifier.of(ViaBedrockUtilityFabric.MOD_ID, "custom-entity");
    private ViaBedrockUtility() {}

    private PayloadHandler payloadHandler;
    private PackManager packManager;
    private boolean viaBedrockPresent;

    public void init() {
        // Register custom entity.
        final EntityType<CustomEntity> type = Registry.register(Registries.ENTITY_TYPE, CUSTOM_ENTITY_IDENTIFIER, EntityType.Builder.create(CustomEntity::new, SpawnGroup.MISC).dimensions(0, 0).build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, CUSTOM_ENTITY_IDENTIFIER)));
        EntityRendererRegistry.register(type, (context) -> new CustomEntityRenderer(context, new CustomEntityModel(new ModelPart(List.of(), Map.of())), Identifier.of("empty")));
        FabricDefaultAttributeRegistry.register(type, CustomEntity.createMobAttributes());

        // Register custom payload.
        this.payloadHandler = new PayloadHandler();
        PayloadTypeRegistry.configurationS2C().register(BasePayload.ID, BasePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(BasePayload.ID, BasePayload.STREAM_CODEC);
        ClientPlayNetworking.registerGlobalReceiver(BasePayload.ID, (payload, context) -> payload.handle(this.payloadHandler));
    }
}
