package org.vivecraft.mixin.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.vivecraft.client.extensions.GuiGraphicsExtension;
import org.vivecraft.client.gui.pip.state.GuiFBTPlayerState;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin implements GuiGraphicsExtension {
    @Shadow
    @Final
    private GuiRenderState guiRenderState;

    @Unique
    @Override
    public void vivecraft$submitFBTRenderState(
        boolean rightReady, boolean leftReady, Vector3fc right, Vector3fc left, int x0, int y0,
        int x1, int y1)
    {
        ScreenRectangle area = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0);
        this.guiRenderState.submitPicturesInPictureState(
            new GuiFBTPlayerState(rightReady, leftReady, right, left, x0, y0,
                x1, y1, 1, area, area));
    }
}
