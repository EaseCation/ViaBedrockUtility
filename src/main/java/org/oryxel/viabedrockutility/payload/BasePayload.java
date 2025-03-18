package org.oryxel.viabedrockutility.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.payload.enums.PayloadType;
import org.oryxel.viabedrockutility.payload.impl.ResourcePackPayload;

import java.nio.charset.StandardCharsets;

public class BasePayload implements CustomPayload {
    public static Id<BasePayload> ID = new Id<>(Identifier.of(ViaBedrockUtilityFabric.MOD_ID, "data"));

    public static final PacketCodec<PacketByteBuf, BasePayload> STREAM_CODEC = PacketCodec.of(null, buf -> {
        final int type = buf.readInt();
        if (type > PayloadType.values().length - 1) {
            throw new RuntimeException("Invalid type: " + type);
        }

        switch (PayloadType.values()[type]) {
            case RESOURCE_PACK -> {
                return new ResourcePackPayload(readString(buf));
            }
            case SPAWN_REQUEST -> {
                return null;
            }
            default -> throw new IllegalStateException("Unexpected value: " + PayloadType.values()[type]);
        }
    });

    public void handle(final PayloadHandler handler) {
        try {
            handler.handle(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
}
