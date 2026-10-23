package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xcompwiz.mystcraft.registry.MystItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** BEWLR for the legacy Book Stand and Lectern inventory/hand models. */
public final class DisplayFurnitureItemRenderer implements SpecialModelRenderer<ItemStack> {
    private final LegacyBookStandBodyRenderer stand = new LegacyBookStandBodyRenderer();

    public DisplayFurnitureItemRenderer() {
    }

    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             SubmitNodeCollector buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (stack.is(MystItems.LECTERN_BLOCK.get())) {
            // Same model-space origin as the world TESR, centered into a normal item cube.
            poseStack.translate(0.5D, 0.20D, 0.5D);
            // CP274: the baked item model already applies the legacy inventory yaw.
            // Do not add the extra 45-degree world yaw here; doing so turned the
            // original sloped lectern silhouette almost edge-on in the GUI.
            LegacyLecternBodyRenderer.render(poseStack, buffers, packedLight);
        } else {
            // Exact RenderBookstand body basis: center, vertical flip, then yaw.
            poseStack.translate(0.5D, 0.70D, 0.5D);
            poseStack.rotate(Axis.ZP.rotationDegrees(180.0F));
            // CP274: same as the original OBJ inventory model: GUI rotation is
            // supplied by the item model, while this renderer only supplies the
            // legacy TESR body basis.  The old extra 45 degrees made the arms read
            // as a flat T instead of the original V-shaped stand icon.
            stand.render(poseStack, buffers, packedLight);
        }
        poseStack.popPose();
    }

    @Override
    public ItemStack extractArgument(ItemStack stack) { return stack.copy(); }

    @Override
    public void submit(ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector,
                       int lightCoords, int overlayCoords, boolean foil, int outlineColor) {
        renderByItem(stack, ItemDisplayContext.FIXED, poseStack, collector, lightCoords, overlayCoords);
    }

    @Override
    public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> output) {
        output.accept(new org.joml.Vector3f(0, 0, 0));
        output.accept(new org.joml.Vector3f(1, 1, 1));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final com.mojang.serialization.MapCodec<Unbaked> CODEC =
                com.mojang.serialization.MapCodec.unit(new Unbaked());
        @Override public com.mojang.serialization.MapCodec<Unbaked> type() { return CODEC; }
        @Override public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.BakingContext context) {
            return new DisplayFurnitureItemRenderer();
        }
    }
}
