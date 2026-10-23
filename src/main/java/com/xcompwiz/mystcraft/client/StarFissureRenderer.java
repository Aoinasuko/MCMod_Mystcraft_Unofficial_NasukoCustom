package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xcompwiz.mystcraft.blockentity.StarFissureBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/** Native projected End portal effect, compressed to the legacy fissure surface. */
public final class StarFissureRenderer extends AbstractEndPortalRenderer<StarFissureBlockEntity, EndPortalRenderState> {
    public StarFissureRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public EndPortalRenderState createRenderState() { return new EndPortalRenderState(); }
    @Override public void submit(EndPortalRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.scale(1, 0.1F, 1);
        submitCube(state.facesToShow, RenderTypes.endPortal(), pose, collector);
        pose.popPose();
    }
}
