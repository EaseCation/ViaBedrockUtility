package org.oryxel.viabedrockutility.payload.impl.camera;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;

import java.nio.charset.StandardCharsets;

/**
 * Payload for the becamera:data channel, handling camera instructions from ViaBedrock.
 */
public class CameraPayload implements CustomPayload {

    public static final Id<CameraPayload> ID = new Id<>(Identifier.of("becamera", "data"));
    public static final String CONFIRM_CHANNEL_ID = "becamera";
    public static final String CONFIRM_CHANNEL_PATH = "confirm";

    public static final PacketCodec<PacketByteBuf, CameraPayload> STREAM_CODEC = PacketCodec.of(null, buf -> {
        final int type = buf.readInt();
        final CameraPayloadType payloadType = CameraPayloadType.values()[type];

        switch (payloadType) {
            case CONFIRM -> {
                ViaBedrockUtilityFabric.LOGGER.info("[BECamera] Received CONFIRM, camera support active!");
                return new CameraPayload(payloadType);
            }
            case CAMERA_INSTRUCTION -> {
                return CameraInstructionPayload.decode(buf);
            }
            case CAMERA_SHAKE -> {
                return CameraShakePayload.decode(buf);
            }
            case CAMERA_PRESETS -> {
                return CameraPresetsPayload.decode(buf);
            }
            default -> throw new IllegalStateException("Unknown camera payload type: " + type);
        }
    });

    private final CameraPayloadType type;

    public CameraPayload(CameraPayloadType type) {
        this.type = type;
    }

    public CameraPayloadType getType() {
        return type;
    }

    public void handle(CameraPayloadHandler handler) {
        handler.handle(this);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static String readString(PacketByteBuf buf) {
        int length = buf.readInt();
        String result = buf.toString(buf.readerIndex(), length, StandardCharsets.UTF_8);
        buf.readerIndex(buf.readerIndex() + length);
        return result;
    }

    /**
     * Must match CameraInterface.PayloadType ordinals in ViaBedrock.
     */
    public enum CameraPayloadType {
        CONFIRM, CAMERA_INSTRUCTION, CAMERA_SHAKE, CAMERA_PRESETS
    }
}
