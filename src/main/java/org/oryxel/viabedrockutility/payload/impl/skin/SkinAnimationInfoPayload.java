package org.oryxel.viabedrockutility.payload.impl.skin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketDecoder;
import org.oryxel.viabedrockutility.payload.BasePayload;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
public final class SkinAnimationInfoPayload extends BasePayload {
    public static final PacketDecoder<PacketByteBuf, SkinAnimationInfoPayload> STREAM_DECODER = buf -> {
        UUID playerUuid = buf.readUuid();
        int animIndex = buf.readInt();
        int type = buf.readInt();
        float frames = buf.readFloat();
        int expression = buf.readInt();
        int width = buf.readInt();
        int height = buf.readInt();
        int chunkCount = buf.readInt();
        return new SkinAnimationInfoPayload(playerUuid, animIndex, type, frames, expression, width, height, chunkCount);
    };

    private final UUID playerUuid;
    private final int animIndex;
    private final int type;
    private final float frames;
    private final int expression;
    private final int width;
    private final int height;
    private final int chunkCount;
}
