package org.oryxel.viabedrockutility.payload.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.oryxel.viabedrockutility.payload.BasePayload;

@RequiredArgsConstructor
@Getter
public class ResourcePackPayload extends BasePayload {
    private final String url;
}
