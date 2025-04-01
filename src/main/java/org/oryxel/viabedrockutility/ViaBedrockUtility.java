package org.oryxel.viabedrockutility;

import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.oryxel.viabedrockutility.pack.PackManager;
import org.oryxel.viabedrockutility.payload.BasePayload;
import org.oryxel.viabedrockutility.payload.PayloadHandler;

@Getter
@Setter
public class ViaBedrockUtility {
    @Getter
    private static final ViaBedrockUtility instance = new ViaBedrockUtility();

    private ViaBedrockUtility() {}

    private PayloadHandler payloadHandler;
    private PackManager packManager;
    private boolean viaBedrockPresent;

    public void init() {
        // Register custom payload.
        this.payloadHandler = new PayloadHandler();
        PayloadTypeRegistry.configurationS2C().register(BasePayload.ID, BasePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(BasePayload.ID, BasePayload.STREAM_CODEC);
        ClientPlayNetworking.registerGlobalReceiver(BasePayload.ID, (payload, context) -> payload.handle(this.payloadHandler));
    }

}
