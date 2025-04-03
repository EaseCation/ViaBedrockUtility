package org.oryxel.viabedrockutility.payload.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.util.Identifier;
import org.cube.converter.data.bedrock.controller.BedrockRenderController;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.oryxel.viabedrockutility.enums.bedrock.ActorFlags;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.mappings.BedrockMappings;
import org.oryxel.viabedrockutility.material.VanillaMaterials;
import org.oryxel.viabedrockutility.material.data.Material;
import org.oryxel.viabedrockutility.mocha.MoLangEngine;
import org.oryxel.viabedrockutility.pack.definitions.EntityDefinitions;
import org.oryxel.viabedrockutility.payload.PayloadHandler;
import org.oryxel.viabedrockutility.payload.impl.entity.ModelRequestPayload;
import org.oryxel.viabedrockutility.renderer.BaseCustomEntityRenderer;
import org.oryxel.viabedrockutility.renderer.extra.CustomEntityRendererImpl;
import org.oryxel.viabedrockutility.util.GeometryUtil;
import team.unnamed.mocha.runtime.Scope;
import team.unnamed.mocha.runtime.binding.JavaObjectBinding;
import team.unnamed.mocha.runtime.standard.MochaMath;
import team.unnamed.mocha.runtime.value.MutableObjectBinding;
import team.unnamed.mocha.runtime.value.Value;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class CustomEntityPayloadHandler extends PayloadHandler {
    private final Map<UUID, Scope> cachedScopes = new ConcurrentHashMap<>();
    private static final Scope BASE_SCOPE = Scope.create();

    private static final Material DEFAULT_MATERIAL;

    static {
        //noinspection UnstableApiUsage
        BASE_SCOPE.set("math", JavaObjectBinding.of(MochaMath.class, null, new MochaMath()));
        BASE_SCOPE.readOnly(true);

        DEFAULT_MATERIAL = VanillaMaterials.getMaterial("entity");
    }

    @Override
    public void handle(ModelRequestPayload payload) {
        final MinecraftClient client = MinecraftClient.getInstance();

        final EntityRendererFactory.Context context = new EntityRendererFactory.Context(client.getEntityRenderDispatcher(),
                client.getItemModelManager(), client.getMapRenderer(), client.getBlockRenderManager(),
                client.getResourceManager(), client.getLoadedEntityModels(), new EquipmentModelLoader(), client.textRenderer);

        if (!this.packManager.getEntityDefinitions().getEntities().containsKey(payload.getIdentifier())) {
            return;
        }

        final EntityDefinitions.EntityDefinition definition = this.packManager.getEntityDefinitions().getEntities().get(payload.getIdentifier());

        final Scope scope = BASE_SCOPE.copy();
        final MutableObjectBinding variableBinding = new MutableObjectBinding();
        scope.set("variable", variableBinding);
        scope.set("v", variableBinding);
        try {
            for (String initExpression : definition.entityData().getScripts().initialize()) {
                MoLangEngine.eval(scope, initExpression);
            }
        } catch (Throwable e) {
            ViaBedrockUtilityFabric.LOGGER.warn("Failed to initialize custom entity variables", e);
        }

        final MutableObjectBinding materialBinding = new MutableObjectBinding();

        for (Map.Entry<String, String> entry : definition.entityData().getMaterials().entrySet()) {
            materialBinding.set(entry.getKey(), Value.of(entry.getKey()));
        }

        materialBinding.block();
        scope.set("material", materialBinding);

        final MutableObjectBinding queryBinding = new MutableObjectBinding();
        if (payload.getEntityData().variant() != null) {
            queryBinding.set("variant", Value.of(payload.getEntityData().variant()));
        }
        if (payload.getEntityData().mark_variant() != null) {
            queryBinding.set("mark_variant", Value.of(payload.getEntityData().mark_variant()));
        }

        final Set<ActorFlags> entityFlags = payload.getEntityData().flags();
        for (Map.Entry<ActorFlags, String> entry : BedrockMappings.getBedrockEntityFlagMoLangQueries().entrySet()) {
            if (entityFlags.contains(entry.getKey())) {
                queryBinding.set(entry.getValue(), Value.of(true));
            }
        }
        if (entityFlags.contains(ActorFlags.ONFIRE)) { // "on fire" flag has two names in MoLang
            queryBinding.set("is_onfire", Value.of(true));
        }

        queryBinding.block();
        scope.set("query", queryBinding);
        scope.set("q", queryBinding);

        scope.readOnly(true);

        this.cachedScopes.put(payload.getUuid(), scope);

        final CustomEntityData data = this.cachedCustomEntities.get(payload.getUuid());
        if (data == null) {
            final Set<String> keySet = new HashSet<>();
            final List<BaseCustomEntityRenderer.Model> models = new CopyOnWriteArrayList<>();
            for (ModelRequestPayload.Model model : payload.getModels()) {
                final String key = model.geometry() + "_" + model.texture() + "_" + model.renderControllerIdentifier();
                final Identifier texture = Identifier.of(model.texture().toLowerCase(Locale.ROOT));

                BedrockGeometryModel geometry = this.packManager.getModelDefinitions().getEntityModels().get(model.geometry());
                if (geometry == null) {
                    continue;
                }

                final BedrockRenderController controller = this.packManager.getRenderControllerDefinitions().getRenderControllers().get(model.renderControllerIdentifier());
                models.add(new BaseCustomEntityRenderer.Model(key, model.geometry(), (EntityModel<?>) GeometryUtil.buildModel(geometry, false, false), texture, evalMaterial(scope, definition, controller)));

                keySet.add(key);
            }

            this.cachedCustomEntities.put(payload.getUuid(), new CustomEntityData(keySet, new CustomEntityRendererImpl<>(models, context)));
        } else {
            final Set<String> old = new HashSet<>(data.availableModels);
            data.availableModels.clear();
            for (ModelRequestPayload.Model model : payload.getModels()) {
                final String key = model.geometry() + "_" + model.texture() + "_" + model.renderControllerIdentifier();
                final Identifier texture = Identifier.of(model.texture().toLowerCase(Locale.ROOT));

                BedrockGeometryModel geometry = this.packManager.getModelDefinitions().getEntityModels().get(model.geometry());
                if (geometry == null) {
                    continue;
                }

                if (old.contains(key)) {
                    data.availableModels.add(key);
                    continue;
                }

                final BedrockRenderController controller = this.packManager.getRenderControllerDefinitions().getRenderControllers().get(model.renderControllerIdentifier());
                data.getRenderer().getModels().add(new BaseCustomEntityRenderer.Model(key, model.geometry(), (EntityModel<?>) GeometryUtil.buildModel(geometry, false, false), texture, evalMaterial(scope, definition, controller)));

                data.availableModels.add(key);
            }

            data.getRenderer().getModels().removeIf(model -> !data.availableModels.contains(model.key()));
        }
    }

    public Material evalMaterial(final Scope baseScope, EntityDefinitions.EntityDefinition definition, BedrockRenderController controller) {
        if (controller == null || controller.materialsMap().isEmpty()) {
            return DEFAULT_MATERIAL;
        }

        // I know it is more complex than just this but this is the case most of the time, and also I don't have the energy.
        if (!controller.materialsMap().containsKey("*")) {
            return DEFAULT_MATERIAL;
        }

        Scope scope = baseScope.copy();
        scope.set("array", this.getArrayBinding(baseScope, controller.materials()));

        try {
            final String materialMapName = MoLangEngine.eval(scope, controller.materialsMap().get("*")).getAsString();
            final String materialName = definition.entityData().getMaterials().get(materialMapName);

            if (materialName == null) {
                return DEFAULT_MATERIAL;
            }

            return this.packManager.getMaterialDefinitions().getMaterial(materialName);
        } catch (IOException ignored) {}

        return DEFAULT_MATERIAL;
    }

    private MutableObjectBinding getArrayBinding(final Scope executionScope, final List<BedrockRenderController.Array> arrays) {
        final MutableObjectBinding arrayBinding = new MutableObjectBinding();
        for (BedrockRenderController.Array array : arrays) {
            if (array.name().toLowerCase(Locale.ROOT).startsWith("array.")) {
                final String[] resolvedExpressions = new String[array.values().size()];
                for (int i = 0; i < array.values().size(); i++) {
                    try {
                        resolvedExpressions[i] = MoLangEngine.eval(executionScope, array.values().get(i)).getAsString();
                    } catch (IOException ignored) {}
                }
                arrayBinding.set(array.name().substring(6), Value.of(resolvedExpressions));
            }
        }
        arrayBinding.block();
        return arrayBinding;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class CustomEntityData {
        private final Set<String> availableModels;
        private CustomEntityRendererImpl<?> renderer;
    }
}
