package org.oryxel.viabedrockpack.pluginmessage.data;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.oryxel.viabedrockpack.ViaBedrockPack;
import org.oryxel.viabedrockpack.pluginmessage.BedrockMessageHandler;

import java.nio.charset.StandardCharsets;

public class BaseData implements CustomPayload {
    public static Id<BaseData> ID = new Id<>(Identifier.of(ViaBedrockPack.MOD_ID, "data"));

    public void handle(BedrockMessageHandler handler) {
        handler.handle(this);
    }

    public static String readString(PacketByteBuf buf) {
        int length = buf.readInt();
        String result = buf.toString(buf.readerIndex(), length, StandardCharsets.UTF_8);
        buf.readerIndex(buf.readerIndex() + length);
        return result;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
