package org.oryxel.viabedrockutility.payload.impl.camera;

import lombok.Getter;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class CameraPresetsPayload extends CameraPayload {

    private final List<PresetEntry> presets;

    private CameraPresetsPayload(List<PresetEntry> presets) {
        super(CameraPayloadType.CAMERA_PRESETS);
        this.presets = presets;
    }

    public static CameraPresetsPayload decode(PacketByteBuf buf) {
        int count = buf.readInt();
        List<PresetEntry> presets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = CameraPayload.readString(buf);
            String parent = CameraPayload.readString(buf);
            Float posX = readOptionalFloat(buf);
            Float posY = readOptionalFloat(buf);
            Float posZ = readOptionalFloat(buf);
            Float rotX = readOptionalFloat(buf);
            Float rotY = readOptionalFloat(buf);
            presets.add(new PresetEntry(name, parent, posX, posY, posZ, rotX, rotY));
        }
        return new CameraPresetsPayload(presets);
    }

    private static Float readOptionalFloat(PacketByteBuf buf) {
        return buf.readBoolean() ? buf.readFloat() : null;
    }

    @Getter
    public static class PresetEntry {
        private final String name;
        private final String parent;
        private final Float posX, posY, posZ;
        private final Float rotX, rotY;

        public PresetEntry(String name, String parent, Float posX, Float posY, Float posZ, Float rotX, Float rotY) {
            this.name = name;
            this.parent = parent;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.rotX = rotX;
            this.rotY = rotY;
        }
    }
}
