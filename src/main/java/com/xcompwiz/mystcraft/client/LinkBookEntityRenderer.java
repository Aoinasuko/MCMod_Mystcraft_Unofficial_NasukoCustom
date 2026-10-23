package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.entity.EntityLinkbook;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** Renders the legacy physical book using the same open-book model as Book Stand/Lectern. */
public final class LinkBookEntityRenderer extends EntityRenderer<EntityLinkbook, MystEntityRenderState> {
    private static final Identifier FALLBACK_TEXTURE =
            Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/entity/linkbook.png");
    private final LegacyBookModelRenderer legacyBook;

    public LinkBookEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.legacyBook = new LegacyBookModelRenderer(context);
        this.shadowRadius = 0.15F;
    }

    @Override
    public MystEntityRenderState createRenderState() { return new MystEntityRenderState(); }

    @Override
    public void extractRenderState(EntityLinkbook entity, MystEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yaw = net.minecraft.util.Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.pitch = net.minecraft.util.Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        state.book = entity.getBook().copy();
    }

    @Override
    public void submit(MystEntityRenderState state, PoseStack poseStack, SubmitNodeCollector buffers,
                       net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        int packedLight = state.lightCoords;
        poseStack.pushPose();
        // 0.13.7.06 RenderLinkbook: y + 1/16, yaw+90 about -Y, Z+90, scale .8.
        poseStack.translate(0.0D, 0.0625D, 0.0D);
        poseStack.rotate(Axis.YP.rotationDegrees(-(state.yaw + 90.0F)));
        poseStack.rotate(Axis.ZP.rotationDegrees(90.0F));
        poseStack.scale(0.8F, 0.8F, 0.8F);
        legacyBook.render(state.book, 1.2F, poseStack, buffers, packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.submit(state, poseStack, buffers, camera);
    }

}
