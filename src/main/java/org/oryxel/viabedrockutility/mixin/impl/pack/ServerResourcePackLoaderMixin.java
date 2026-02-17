package org.oryxel.viabedrockutility.mixin.impl.pack;

import net.minecraft.client.resource.server.ReloadScheduler;
import net.minecraft.client.resource.server.ServerResourcePackLoader;
import net.minecraft.resource.ResourcePackProfile;
import org.oryxel.viabedrockutility.ViaBedrockUtility;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.pack.PackManager;
import org.oryxel.viabedrockutility.pack.content.Content;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Mixin(ServerResourcePackLoader.class)
public class ServerResourcePackLoaderMixin {
    @Inject(method = "toProfiles", at = @At("HEAD"))
    private void toProfiles(List<ReloadScheduler.PackInfo> packs, CallbackInfoReturnable<List<ResourcePackProfile>> cir) {
        if (!ViaBedrockUtility.getInstance().isViaBedrockPresent()) {
            return;
        }

        ViaBedrockUtilityFabric.LOGGER.info("[ResourcePack] Intercepting server resource packs, {} pack(s) received", packs.size());
        final List<Content> contents = new ArrayList<>();
        packs.stream().map(ReloadScheduler.PackInfo::path).forEach(pack -> {
            try {
                final Content content = new Content(Files.readAllBytes(pack));
                final List<String> mcpacks = content.getFilesDeep("bedrock/", ".mcpack");
                ViaBedrockUtilityFabric.LOGGER.info("[ResourcePack] Found {} bedrock mcpack(s) in {}", mcpacks.size(), pack.getFileName());
                for (final String path : mcpacks) {
                    ViaBedrockUtilityFabric.LOGGER.info("[ResourcePack]   - {}", path);
                    contents.add(new Content(content.get(path)));
                }
            } catch (IOException e) {
                ViaBedrockUtilityFabric.LOGGER.warn("[ResourcePack] Failed to read pack {}", pack);
            }
        });

        ViaBedrockUtilityFabric.LOGGER.info("[ResourcePack] Loaded {} bedrock pack(s) total, initializing PackManager", contents.size());
        ViaBedrockUtility.getInstance().setPackManager(new PackManager(contents));
    }
}
