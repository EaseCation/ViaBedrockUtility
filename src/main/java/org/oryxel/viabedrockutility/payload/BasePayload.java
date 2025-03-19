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
import org.oryxel.viabedrockutility.payload.impl.entity.*;
import org.oryxel.viabedrockutility.payload.impl.skin.*;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Getter
public class BasePayload implements CustomPayload {
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
                return new BasePayload();
            }
            case SPAWN_REQUEST -> {
                return new SpawnRequestPayload(BasePayload.readString(buf), BasePayload.readString(buf), buf.readVarInt(), buf.readUuid(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readByte(), buf.readByte(), buf.readByte());
            }

            case ANIMATE -> {
                // TODO: Implement this.
                return new BasePayload();
            }

            case CAPE -> {
                return CapeDataPayload.STREAM_DECODER.decode(buf);
            }
            case SKIN_INFORMATION -> {
                return BaseSkinPayload.STREAM_DECODER.decode(buf);
            }
            case SKIN_DATA -> {
                return SkinDataPayload.STREAM_DECODER.decode(buf);
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
