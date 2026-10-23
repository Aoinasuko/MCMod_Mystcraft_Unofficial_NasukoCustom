package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xcompwiz.mystcraft.block.BlockBookReceptacle;
import com.xcompwiz.mystcraft.blockentity.BookReceptacleBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Restores the closed book sitting in the 0.13.7.06 Book Receptacle. */
public final class BookReceptacleRenderer implements BlockEntityRenderer<BookReceptacleBlockEntity, MystFurnitureRenderState> {
    private final net.minecraft.client.renderer.item.ItemModelResolver itemModels;
    private final LegacyBookModelRenderer legacyBook;

    public BookReceptacleRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModels = context.itemModelResolver();
        this.legacyBook = new LegacyBookModelRenderer(context);
    }

    @Override
    public MystFurnitureRenderState createRenderState() { return new MystFurnitureRenderState(); }

    @Override
    public void extractRenderState(BookReceptacleBlockEntity blockEntity, MystFurnitureRenderState state,
                                   float partialTick, net.minecraft.world.phys.Vec3 cameraPosition,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        state.state = blockEntity.getBlockState();
        state.book = blockEntity.getBook().copy();
        itemModels.updateForTopItem(state.item, state.book, ItemDisplayContext.FIXED,
                blockEntity.getLevel(), null, 0);
    }

    @Override
    public void submit(MystFurnitureRenderState state, PoseStack poseStack,
                       SubmitNodeCollector buffers, net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        int packedLight = state.lightCoords;
        int packedOverlay = OverlayTexture.NO_OVERLAY;
        ItemStack stack = state.book;
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        Direction rotation = state.state.getValue(BlockBookReceptacle.ROTATION);
        switch (rotation) {
            case UP -> {
                poseStack.rotate(Axis.XP.rotationDegrees(-90.0F));
                poseStack.rotate(Axis.YP.rotationDegrees(90.0F));
            }
            case NORTH -> poseStack.rotate(Axis.YP.rotationDegrees(-90.0F));
            case SOUTH -> poseStack.rotate(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.rotate(Axis.YP.rotationDegrees(180.0F));
            case DOWN, WEST -> { }
        }
        poseStack.scale(0.8F, 0.8F, 0.8F);

        if (legacyBook.canRender(stack)) {
            legacyBook.render(stack, 0.0F, poseStack, buffers, packedLight, packedOverlay);
        } else {
            state.item.submit(poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY, 0);
        }
        poseStack.popPose();
    }
}
