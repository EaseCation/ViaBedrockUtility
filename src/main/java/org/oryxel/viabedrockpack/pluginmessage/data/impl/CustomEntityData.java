package org.oryxel.viabedrockpack.pluginmessage.data.impl;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.util.Identifier;
import org.oryxel.viabedrockpack.pluginmessage.data.BaseData;

@RequiredArgsConstructor
@Getter
public final class CustomEntityData extends BaseData {
    private final String name;
    private final int geometryIndex;
    private final Identifier texture;
    private final JsonObject geometryData;
}