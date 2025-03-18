package org.oryxel.viabedrockutility.payload;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.oryxel.viabedrockutility.ViaBedrockUtility;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.payload.enums.PayloadType;
import org.oryxel.viabedrockutility.payload.impl.SpawnRequestPayload;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Getter
public class BasePayload implements CustomPayload {
    private final PayloadType type;

    public static Id<BasePayload> ID = new Id<>(Identifier.of(ViaBedrockUtilityFabric.MOD_ID, "data"));

    public static final PacketCodec<PacketByteBuf, BasePayload> STREAM_CODEC = PacketCodec.of(null, buf -> {
        final int type = buf.readInt();
        if (type > PayloadType.values().length - 1) {
            throw new RuntimeException("Invalid type: " + type);
        }

        switch (PayloadType.values()[type]) {
            case CONFIRM -> {
                // Confirm that ViaBedrock is present, this should be sent back right after we send confirm register channel.
                ViaBedrockUtility.getInstance().setViaBedrockPresent(true);
                return new BasePayload(PayloadType.CONFIRM);
            }
            case SPAWN_REQUEST -> {
                return new SpawnRequestPayload(PayloadType.SPAWN_REQUEST, BasePayload.readString(buf), BasePayload.readString(buf), buf.readVarInt(), buf.readUuid(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readByte(), buf.readByte(), buf.readByte());
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
