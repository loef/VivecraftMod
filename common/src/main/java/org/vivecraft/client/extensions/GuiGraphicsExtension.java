package org.vivecraft.client.extensions;

import org.joml.Vector3fc;

public interface GuiGraphicsExtension {
    void vivecraft$submitFBTRenderState(
        boolean rightReady, boolean leftReady, Vector3fc right, Vector3fc left, int x0, int y0, int x1, int y1);
}
