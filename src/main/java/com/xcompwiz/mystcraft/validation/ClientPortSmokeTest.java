package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.block.BlockWritingDesk;
import com.xcompwiz.mystcraft.blockentity.*;
import com.xcompwiz.mystcraft.item.*;
import com.xcompwiz.mystcraft.linking.*;
import com.xcompwiz.mystcraft.network.AgeClientVisualState;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.*;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.List;

/** Creates its own disposable world only with -Dmystcraft.clientValidation=true. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT)
public final class ClientPortSmokeTest {
    private static boolean started, linked, panelClicked, furnitureRequested;
    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> sourceDimension;
    private static ItemStack sceneBook;
    private static volatile int droppedEntityId = -1;
    private static int ticks, ageTicks;
    private static volatile boolean menuReady;
    private static BlockPos deskPos;
    private static final String WORLD = "NasukoPortValidation";

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("mystcraft.clientValidation")) return;
        var mc = Minecraft.getInstance();
        if (!started && mc.isGameLoadFinished() && mc.gui.screen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.gui.setScreen(new TitleScreen());
        }
        if (!started && mc.gui.screen() instanceof TitleScreen) {
            started = true;
            if (mc.getLevelSource().levelExists(WORLD)) mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("Smoke world failed to open"); });
            else mc.createWorldOpenFlows().createFreshLevel(WORLD,
                    new LevelSettings(WORLD, GameType.CREATIVE, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
                    new WorldOptions(630025L, true, false), WorldPresets::createNormalWorldDimensions, new TitleScreen());
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (++ticks > 2400) throw new IllegalStateException("Client validation timed out");
        var server = mc.getSingleplayerServer(); var uuid = mc.player.getUUID();
        if (!linked && ticks > 60) {
            linked = true;
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                var book = new ItemStack(MystItems.AGEBOOK.get());
                var terrain = new ItemStack(MystItems.PAGE.get()); Page.setSymbolId(terrain, "mystcraft:TerrainNormal");
                var linkPage = Page.createLinkPage(); Page.addLinkProperty(linkPage, LinkProperties.GENERATE_PLATFORM);
                ItemAgebook.create(book, player, List.of(linkPage, terrain), "Nasuko Smoke Age");
                sceneBook = book;
                player.setItemInHand(InteractionHand.MAIN_HAND, book);
                book.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
                Mystcraft.LOGGER.info("CLIENT SMOKE: opened actual held-book menu");
            });
        }
        if (!panelClicked && mc.gui.screen() instanceof com.xcompwiz.mystcraft.client.LinkBookScreen screen) {
            sourceDimension = mc.level.dimension();
            capture(mc, "nasuko-link-panel.png");
            var click = new net.minecraft.client.input.MouseButtonEvent((screen.width - 327) / 2 + 239,
                    (screen.height - 199) / 2 + 60,
                    new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
            if (!screen.mouseClicked(click, false)) throw new IllegalStateException("Left click did not activate Link Panel");
            panelClicked = true;
        }
        if (!panelClicked || mc.level.dimension().equals(sourceDimension)
                || AgeClientVisualState.get(mc.level.dimension().identifier().toString()) == null) return;
        if (!furnitureRequested) {
            furnitureRequested = true;
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                var book = sceneBook;
                var terrain = new ItemStack(MystItems.PAGE.get()); Page.setSymbolId(terrain, "mystcraft:TerrainNormal");
                AgeManager.resolveByLevel(server, player.level().dimension()).orElseThrow().setInstabilityEnabled(false);
                player.getAbilities().flying = true; player.onUpdateAbilities();
                var level = player.level(); deskPos = player.blockPosition().offset(2, 0, 0);
                for (int x=-5;x<=5;x++) for(int z=-5;z<=5;z++) level.setBlockAndUpdate(player.blockPosition().offset(x,-1,z), Blocks.STONE.defaultBlockState());
                var desk = MystBlocks.WRITING_DESK.get().defaultBlockState().setValue(BlockWritingDesk.FACING, Direction.NORTH);
                level.setBlock(deskPos, desk, 2); level.setBlock(deskPos.north(), desk.setValue(BlockWritingDesk.IS_FOOT, true), 2);
                var be = (WritingDeskBlockEntity) level.getBlockEntity(deskPos); be.setInkAmount(1000); be.setItem(0, terrain.copy());
                level.setBlockAndUpdate(deskPos.south(2), MystBlocks.LECTERN.get().defaultBlockState());
                if (level.getBlockEntity(deskPos.south(2)) instanceof BookDisplayBlockEntity display) display.insertBook(book.copy());
                player.getInventory().setItem(1, terrain.copy()); player.getInventory().setItem(2, ItemWritingDesk.topStack());
                player.getInventory().setItem(3, new ItemStack(MystItems.LECTERN_BLOCK.get()));
                player.getInventory().setItem(4, new ItemStack(MystItems.BOOK_STAND_BLOCK.get()));
                player.getInventory().setItem(5, new ItemStack(MystItems.INK_VIAL.get()));
                Mystcraft.LOGGER.info("CLIENT SMOKE: book travel completed into {}", level.dimension().identifier());
            });
        }
        ageTicks++;
        if (ageTicks == 20) server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            player.setItemInHand(InteractionHand.MAIN_HAND, sceneBook.copy());
            player.drop(false); // The same selected-item path used by the Q key.
        });
        // NeoForge queues custom ItemEntity replacement on the server executor.
        if (ageTicks == 40) server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            var dropped = player.level().getEntitiesOfClass(com.xcompwiz.mystcraft.entity.EntityLinkbook.class,
                    player.getBoundingBox().inflate(8));
            if (dropped.isEmpty()) throw new IllegalStateException("Dropping a book did not create EntityLinkbook");
            droppedEntityId = dropped.getFirst().getId();
            Mystcraft.LOGGER.info("CLIENT SMOKE: dropped physical book {}", droppedEntityId);
        });
        if (ageTicks == 100) {
            if (!(mc.level.getEntity(droppedEntityId) instanceof com.xcompwiz.mystcraft.entity.EntityLinkbook physical)
                    || physical.tickCount < 20) throw new IllegalStateException("Dropped book did not survive client ticking");
            Mystcraft.LOGGER.info("CLIENT SMOKE: dropped book survived {} client ticks", physical.tickCount);
        }
        if (ageTicks == 120) capture(mc, "nasuko-age.png");
        if (ageTicks == 150) server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            var desk = (WritingDeskBlockEntity) player.level().getBlockEntity(deskPos);
            open(player, deskPos); menuReady = true;
        });
        if (ageTicks == 210) {
            if (!menuReady || !(mc.gui.screen() instanceof com.xcompwiz.mystcraft.client.WritingDeskScreen)) throw new IllegalStateException("Writing Desk screen failed to open");
            capture(mc, "nasuko-writing-desk.png");
        }
        if (ageTicks == 240) server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid); player.closeContainer();
            var pos = deskPos.south(3); player.level().setBlockAndUpdate(pos, MystBlocks.BOOK_BINDER.get().defaultBlockState());
            var binder = (BookBinderBlockEntity) player.level().getBlockEntity(pos);
            binder.setItem(0, new ItemStack(Items.LEATHER)); binder.insertPage(Page.createLinkPage(),0,true); binder.setPendingTitle("Nasuko Test"); open(player, pos);
        });
        if (ageTicks == 300) capture(mc, "nasuko-book-binder.png");
        if (ageTicks == 340) {
            Mystcraft.LOGGER.info("CLIENT SMOKE PASS: real Link Panel left click, network Age travel, dropped-book client ticking, rendering and station screens");
            mc.stop();
        }
    }
    private static void open(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        player.level().getBlockState(pos).useWithoutItem(player.level(), player,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }
    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1, message -> Mystcraft.LOGGER.info("CLIENT SMOKE screenshot: {}", message.getString()));
    }
}
