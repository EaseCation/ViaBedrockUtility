package org.oryxel.viabedrockutility.payload.impl.skin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketDecoder;
import org.oryxel.viabedrockutility.payload.BasePayload;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
public final class BaseSkinPayload extends BasePayload {
    public static final PacketDecoder<PacketByteBuf, BaseSkinPayload> STREAM_DECODER = buf -> {
        final UUID playerUuid = buf.readUuid();

        int skinWidth = buf.readInt(), skinHeight = buf.readInt();

        String geometry = "";
        if (buf.readBoolean()) {
            geometry = BasePayload.readString(buf);
        }

        return new BaseSkinPayload(playerUuid, skinWidth, skinHeight, geometry, buf.readInt());
    };

    private final UUID playerUuid;
    private final int skinWidth;
    private final int skinHeight;
    private final String geometry;
    private final int chunkCount;
}