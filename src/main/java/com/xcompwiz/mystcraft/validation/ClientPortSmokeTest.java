package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.block.BlockWritingDesk;
import com.xcompwiz.mystcraft.blockentity.*;
import com.xcompwiz.mystcraft.item.*;
import com.xcompwiz.mystcraft.linking.*;
import com.xcompwiz.mystcraft.network.AgeClientVisualState;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.*;
import com.xcompwiz.mystcraft.entity.monster.*;
import net.minecraft.world.entity.EntitySpawnReason;
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
    private static volatile int pterosaurId = -1, serpentId = -1;
    private static volatile int fairyId = -1, wormId = -1, calamityId = -1, orbId = -1;
    private static volatile BlockPos reinforcementCenter;
    private static int laserSeenAt = -1;
    private static boolean circleSeen;
    private static int ticks, ageTicks;
    private static volatile boolean menuReady;
    private static boolean previousHideGui;
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
                var monsters = new ItemStack(MystItems.PAGE.get()); Page.setSymbolId(monsters, "mystcraft:DimensionalMonsters");
                var light = new ItemStack(MystItems.PAGE.get()); Page.setSymbolId(light, "mystcraft:LightingBright");
                ItemAgebook.create(book, player, List.of(linkPage, terrain, monsters, monsters.copy(), monsters.copy(), light), "Nasuko Smoke Age");
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
        if (ageTicks == 340) server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid); player.closeContainer();
            server.setDifficulty(Difficulty.NORMAL, true);
            var level = player.level(); var center = deskPos.south(16).above(2);
            for (int x = -8; x <= 8; x++) for (int z = -10; z <= 8; z++) {
                level.setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 0; y <= 8; y++) level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
            var p = MystEntities.DIMENSIONAL_PTEROSAUR.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            p.setPos(center.getX() - 2, center.getY() + 2.5, center.getZ());
            p.setNoAi(true); p.setYRot(135); p.yBodyRot = 135; p.setPersistenceRequired(); level.addFreshEntity(p); pterosaurId = p.getId();
            var s = MystEntities.DIMENSIONAL_SERPENT.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            s.setPos(center.getX() + 2, center.getY(), center.getZ());
            s.setNoAi(true); s.setYRot(135); s.yBodyRot = 135; s.setPersistenceRequired(); level.addFreshEntity(s); serpentId = s.getId();
            player.teleportTo(level, center.getX() + .5, center.getY() + 1.5, center.getZ() - 7,
                    java.util.Set.of(), 0, 10, true);
            var page = new ItemStack(MystItems.PAGE.get()); Page.setSymbolId(page, "mystcraft:DimensionalMonsters");
            player.setItemInHand(InteractionHand.MAIN_HAND, page);
            player.getInventory().setItem(7, new ItemStack(MystItems.PTEROSAUR_EGG.get()));
            player.getInventory().setItem(8, new ItemStack(MystItems.SERPENT_EGG.get()));
        });
        if (ageTicks == 420) { previousHideGui = mc.gui.hud.isHidden(); if (!previousHideGui) mc.gui.hud.toggle(); }
        if (ageTicks == 440) {
            if (!(mc.level.getEntity(pterosaurId) instanceof com.xcompwiz.mystcraft.entity.monster.DimensionalPterosaur p)
                    || !(mc.level.getEntity(serpentId) instanceof com.xcompwiz.mystcraft.entity.monster.DimensionalSerpent s)
                    || p.tickCount < 30 || s.tickCount < 30) throw new IllegalStateException("Monster client synchronization failed");
            capture(mc, "nasuko-dimensional-monsters.png");
            Mystcraft.LOGGER.info("CLIENT SMOKE: both Blender models survived client rendering; pterosaur ticks={}, serpent ticks={}", p.tickCount, s.tickCount);
        }
        if (ageTicks == 450 && mc.gui.hud.isHidden() != previousHideGui) mc.gui.hud.toggle();
        if (ageTicks == 480) server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid); var level = player.level();
            var center = deskPos.south(44).above(3); reinforcementCenter = center;
            for (int x=-13; x<=13; x++) for (int z=-11; z<=13; z++) {
                level.setBlock(center.offset(x,-1,z), Blocks.STONE.defaultBlockState(), 2);
                for (int y=0; y<=10; y++) level.setBlock(center.offset(x,y,z), Blocks.AIR.defaultBlockState(), 2);
            }
            var f = MystEntities.DIMENSIONAL_FAIRY.get().create(level, EntitySpawnReason.TRIGGERED);
            f.setPos(center.getX()-5, center.getY()+2, center.getZ()); f.setNoAi(true); f.setYRot(150); f.setPersistenceRequired(); level.addFreshEntity(f); fairyId = f.getId();
            var w = MystEntities.DIMENSIONAL_WORM.get().create(level, EntitySpawnReason.TRIGGERED);
            w.setPos(center.getX(), center.getY(), center.getZ()); w.setNoAi(true); w.setYRot(150); w.setPersistenceRequired(); level.addFreshEntity(w); wormId = w.getId();
            var c = MystEntities.DIMENSIONAL_CALAMITY.get().create(level, EntitySpawnReason.TRIGGERED);
            c.setPos(center.getX()+5, center.getY()+.5, center.getZ()); c.setNoAi(true); c.setYRot(150); c.setPersistenceRequired(); level.addFreshEntity(c); calamityId = c.getId();
            var orb = MystEntities.CALAMITY_ORB.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            orb.setOwner(c); orb.setPos(center.getX()+5, center.getY()+4.5, center.getZ()); level.addFreshEntity(orb); orbId = orb.getId();
            player.teleportTo(level, center.getX()+.5, center.getY()+2, center.getZ()-12, java.util.Set.of(), 0, 10, true);
            player.getInventory().setItem(6,new ItemStack(MystItems.FAIRY_EGG.get()));
            player.getInventory().setItem(7,new ItemStack(MystItems.WORM_EGG.get()));
            player.getInventory().setItem(8,new ItemStack(MystItems.CALAMITY_EGG.get()));
        });
        if (ageTicks == 580) server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid); var level = player.level(); var center = reinforcementCenter;
            var circle = MystEntities.SUMMONING_CIRCLE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            circle.setPos(center.getX()-2, center.getY()+.03, center.getZ()-2);
            circle.configure((DimensionalCalamity)level.getEntity(calamityId), 2); level.addFreshEntity(circle);
        });
        if (ageTicks == 590) { previousHideGui = mc.gui.hud.isHidden(); if (!previousHideGui) mc.gui.hud.toggle(); }
        if (ageTicks >= 590 && ageTicks <= 610 && ageTicks % 10 == 0) server.execute(() -> {
            var level = server.getPlayerList().getPlayer(uuid).level(); var center = reinforcementCenter;
            var laser = MystEntities.DIMENSIONAL_LASER.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            laser.setPos(center.getX()-5, center.getY()+2.65, center.getZ());
            laser.setEnd(laser.position().add(11,-1.8,-4)); level.addFreshEntity(laser);
        });
        if (ageTicks >= 580 && ageTicks <= 620) for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof DimensionalSummoningCircle) circleSeen = true;
            if (entity instanceof DimensionalLaser beam && laserSeenAt < 0 && entity.tickCount > 0) {
                if (beam.endOffset().length() < 10 || !beam.shouldRenderAtSqrDistance(mc.player.distanceToSqr(beam)))
                    throw new IllegalStateException("Laser endpoint or draw distance did not synchronize");
                laserSeenAt = ageTicks;
            }
        }
        if (laserSeenAt >= 0 && ageTicks == laserSeenAt + 4) capture(mc, "nasuko-dimensional-reinforcements-spells.png");
        if (ageTicks == 620) {
            if (!(mc.level.getEntity(fairyId) instanceof DimensionalFairy f) || f.tickCount < 60
                    || !(mc.level.getEntity(wormId) instanceof DimensionalWorm w) || w.tickCount < 60
                    || !(mc.level.getEntity(calamityId) instanceof DimensionalCalamity c) || c.tickCount < 60
                    || !(mc.level.getEntity(orbId) instanceof CalamityOrb) || !circleSeen || laserSeenAt < 0)
                throw new IllegalStateException("Reinforcement models/projectile/spells failed client synchronization");
            capture(mc, "nasuko-dimensional-reinforcements.png");
            Mystcraft.LOGGER.info("CLIENT SMOKE: fairy, worm, calamity, orb, laser and circle rendered successfully");
        }
        if (ageTicks == 630 && mc.gui.hud.isHidden() != previousHideGui) mc.gui.hud.toggle();
        if (ageTicks == 650) {
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid); var level = player.level();
                var pos = player.blockPosition().south(3);
                var desk = MystBlocks.WRITING_DESK.get().defaultBlockState().setValue(BlockWritingDesk.FACING,Direction.NORTH);
                level.setBlock(pos,desk,2); level.setBlock(pos.north(),desk.setValue(BlockWritingDesk.IS_FOOT,true),2);
                var villager = net.minecraft.world.entity.EntityTypes.VILLAGER.create(level,EntitySpawnReason.TRIGGERED);
                villager.setVillagerData(villager.getVillagerData().withProfession(MystVillagers.ARCHIVIST));
                villager.setPos(pos.getX()+1.5,pos.getY(),pos.getZ()+.5); villager.setNoAi(true); level.addFreshEntity(villager);
                player.interactOn(villager,InteractionHand.MAIN_HAND,villager.position());
            });
        }
        if (ageTicks == 710) {
            if (!(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen screen)
                    || screen.getMenu().getOffers().size()!=4) throw new IllegalStateException("Archivist did not open vanilla merchant screen with four offers");
            capture(mc,"nasuko-archivist-vanilla-trading.png");
            Mystcraft.LOGGER.info("CLIENT SMOKE: native Archivist MerchantScreen and four offers synchronized");
        }
        if (ageTicks == 730) {
            Mystcraft.LOGGER.info("CLIENT SMOKE PASS: book travel/drop, station screens, five Blender monsters/spells, native Archivist trading screen");
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
