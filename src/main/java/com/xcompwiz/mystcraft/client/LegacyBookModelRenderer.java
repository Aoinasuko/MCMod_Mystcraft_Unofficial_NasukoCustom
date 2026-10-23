package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.item.ItemLinking;
import com.xcompwiz.mystcraft.registry.MystItems;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Shared modern renderer for the legacy 3D Mystcraft book model. */
final class LegacyBookModelRenderer {
    private static final Identifier AGEBOOK_TEXTURE =
            Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/entity/agebook.png");
    private static final Identifier LINKBOOK_TEXTURE =
            Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/entity/linkbook.png");

    private final BookModel bookModel;

    LegacyBookModelRenderer(BlockEntityRendererProvider.Context context) {
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    LegacyBookModelRenderer(EntityRendererProvider.Context context) {
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    boolean canRender(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemLinking;
    }

    void render(ItemStack stack, float openAmount, PoseStack poseStack, SubmitNodeCollector buffers,
                int packedLight, int packedOverlay) {
        if (!canRender(stack)) return;
        Identifier texture = stack.is(MystItems.AGEBOOK.get()) ? AGEBOOK_TEXTURE : LINKBOOK_TEXTURE;
        buffers.submitModel(bookModel, BookModel.State.forAnimation(0.0F, 0.0F, 0.0F, openAmount),
                poseStack, texture, packedLight, OverlayTexture.NO_OVERLAY, 0);
    }
}
