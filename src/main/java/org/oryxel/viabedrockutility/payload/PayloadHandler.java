package org.oryxel.viabedrockutility.payload;

import org.oryxel.viabedrockutility.ViaBedrockUtility;
import org.oryxel.viabedrockutility.pack.PackManager;
import org.oryxel.viabedrockutility.pack.content.Content;
import org.oryxel.viabedrockutility.payload.impl.ResourcePackPayload;
import org.oryxel.viabedrockutility.util.NetworkUtil;

import java.net.URL;

public class PayloadHandler {
    private PackManager packManager;

    public void handle(final BasePayload payload) throws Exception {
        if (payload instanceof ResourcePackPayload resource) {
            handle(resource);
        }
    }

    public void handle(final ResourcePackPayload payload) throws Exception {
        final URL url = new URL(payload.getUrl());
        final byte[] data = NetworkUtil.download(url);

        ViaBedrockUtility.getInstance().setPackManager(this.packManager = new PackManager(new Content(data)));
    }
}
