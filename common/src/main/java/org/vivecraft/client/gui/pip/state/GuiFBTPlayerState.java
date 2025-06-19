package org.vivecraft.client.gui.pip.state;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.joml.Vector3fc;

public record GuiFBTPlayerState(boolean rightReady, boolean leftReady, Vector3fc right, Vector3fc left, int x0, int y0,
                                int x1, int y1, float scale, ScreenRectangle scissorArea,
                                ScreenRectangle bounds) implements PictureInPictureRenderState
{}
