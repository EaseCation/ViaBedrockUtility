package org.oryxel.viabedrockpack.pluginmessage.data.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.oryxel.viabedrockpack.pluginmessage.data.BaseData;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class SpawnEntityData extends BaseData {
    private final String name;
    private final UUID uuid;
    private final int entityId;
    private final double x;
    private final double y;
    private final double z;
    private final byte yaw;
    private final byte pitch;
}
