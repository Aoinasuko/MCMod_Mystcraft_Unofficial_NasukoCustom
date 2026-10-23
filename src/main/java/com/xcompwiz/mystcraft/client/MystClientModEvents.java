package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.registry.MystMenus;
import com.xcompwiz.mystcraft.registry.MystBlockEntities;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.registry.MystEntities;
import com.xcompwiz.mystcraft.world.dimension.AgeDimensionKeys;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.world.level.block.Blocks;



/** Client MOD-bus registration events; runtime viewport/network events stay in MystClientEvents. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT)
public final class MystClientModEvents {
    private MystClientModEvents() {}

    @SubscribeEvent
    public static void registerSpecialModels(net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent event) {
        event.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "page"), PageItemRenderer.Unbaked.CODEC);
        event.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "display_furniture"), DisplayFurnitureItemRenderer.Unbaked.CODEC);
    }

    @SubscribeEvent public static void registerScreens(RegisterMenuScreensEvent e) {
        e.register(MystMenus.BOOK.get(), LinkBookScreen::new);
        e.register(MystMenus.BOOK_BINDER.get(), BookBinderScreen::new);
        e.register(MystMenus.WRITING_DESK.get(), WritingDeskScreen::new);
        e.register(MystMenus.INK_MIXER.get(), InkMixerScreen::new);
        e.register(MystMenus.LINK_MODIFIER.get(), LinkModifierScreen::new);
        e.register(MystMenus.ARCHIVIST.get(), ArchivistShopScreen::new);
    }


    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(MystEntities.LINKBOOK.get(), LinkBookEntityRenderer::new);
        e.registerEntityRenderer(MystEntities.METEOR.get(), MeteorEntityRenderer::new);
        e.registerEntityRenderer(MystEntities.DIMENSIONAL_PTEROSAUR.get(), c -> new DimensionalMonsterRenderer<>(c, true));
        e.registerEntityRenderer(MystEntities.DIMENSIONAL_SERPENT.get(), c -> new DimensionalMonsterRenderer<>(c, false));
        e.registerEntityRenderer(MystEntities.DIMENSIONAL_FAIRY.get(), c -> new DimensionalMonsterRenderer<>(c, "dimensional_fairy"));
        e.registerEntityRenderer(MystEntities.DIMENSIONAL_WORM.get(), c -> new DimensionalMonsterRenderer<>(c, "dimensional_worm"));
        e.registerEntityRenderer(MystEntities.DIMENSIONAL_CALAMITY.get(), c -> new DimensionalMonsterRenderer<>(c, "dimensional_calamity"));
        e.registerEntityRenderer(MystEntities.CALAMITY_ORB.get(), c -> new DimensionalMonsterRenderer<>(c, "calamity_orb"));
        e.registerEntityRenderer(MystEntities.DIMENSIONAL_LASER.get(), c -> new DimensionalSpellRenderer<>(c, true));
        e.registerEntityRenderer(MystEntities.SUMMONING_CIRCLE.get(), c -> new DimensionalSpellRenderer<>(c, false));
        e.registerBlockEntityRenderer(MystBlockEntities.BOOK_DISPLAY.get(), BookDisplayRenderer::new);
        e.registerBlockEntityRenderer(MystBlockEntities.BOOK_RECEPTACLE.get(), BookReceptacleRenderer::new);
        e.registerBlockEntityRenderer(MystBlockEntities.WRITING_DESK.get(), WritingDeskRenderer::new);
        e.registerBlockEntityRenderer(MystBlockEntities.STAR_FISSURE.get(), StarFissureRenderer::new);
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(java.util.List.of(new net.minecraft.client.color.block.BlockTintSource() {
            public int color(net.minecraft.world.level.block.state.BlockState state) { return -1; }
            public int colorInWorld(net.minecraft.world.level.block.state.BlockState state,
                                    net.minecraft.client.renderer.block.BlockAndTintGetter level,
                                    net.minecraft.core.BlockPos pos) {
                return 0xFF000000 | CrystalPortalColorHandler.color(state, level, pos, 0);
            }
        }), com.xcompwiz.mystcraft.registry.MystBlocks.LINK_PORTAL.get());
    }

    @SubscribeEvent
    public static void registerFluidModels(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
        var still = new net.minecraft.client.resources.model.sprite.Material(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "block/black_ink_still"));
        var flowing = new net.minecraft.client.resources.model.sprite.Material(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "block/black_ink_flow"));
        var tint = net.neoforged.neoforge.client.fluid.FluidTintSources.constant(0xFF191919);
        event.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(still, flowing, null, tint),
                com.xcompwiz.mystcraft.registry.MystFluids.BLACK_INK, com.xcompwiz.mystcraft.registry.MystFluids.FLOWING_BLACK_INK);
    }
    @SubscribeEvent
    public static void registerSkyPipelines(net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) {
        AgeSkyGpuUploader.register(event);
    }
}
