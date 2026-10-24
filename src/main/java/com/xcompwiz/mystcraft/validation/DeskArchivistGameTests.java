package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.block.BlockWritingDesk;
import com.xcompwiz.mystcraft.blockentity.WritingDeskBlockEntity;
import com.xcompwiz.mystcraft.item.ItemWritingDesk;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.*;
import com.xcompwiz.mystcraft.villager.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.*;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native break, employment, merchant menu, stock, save and network regressions. */
final class DeskArchivistGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION,"mystcraft");
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("mystcraft",path); }
    static void register(IEventBus bus) {
        FUNCTIONS.register("desk_single_drop", () -> DeskArchivistGameTests::drops);
        FUNCTIONS.register("archivist_native_trades", () -> DeskArchivistGameTests::trades);
        FUNCTIONS.register("archivist_workstation", () -> DeskArchivistGameTests::workstation);
        FUNCTIONS.register(bus); bus.addListener(DeskArchivistGameTests::tests);
    }
    private static void tests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(id("desk_archivist_validation"));
        for (String name : List.of("desk_single_drop","archivist_native_trades","archivist_workstation"))
            event.registerTest(id(name),new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id(name)),
                    new TestData<>(env,id("port_empty"),1500,0,true)));
    }
    private static void place(GameTestHelper h, BlockPos main, Direction facing, boolean top) {
        var state=MystBlocks.WRITING_DESK.get().defaultBlockState().setValue(BlockWritingDesk.FACING,facing);
        h.getLevel().setBlock(main,state,2); h.getLevel().setBlock(main.relative(facing),state.setValue(BlockWritingDesk.IS_FOOT,true),2);
        if(top) {
            h.getLevel().setBlock(main.above(),state.setValue(BlockWritingDesk.IS_TOP,true),2);
            h.getLevel().setBlock(main.relative(facing).above(),state.setValue(BlockWritingDesk.IS_TOP,true).setValue(BlockWritingDesk.IS_FOOT,true),2);
        }
    }
    private static ServerPlayer connectedPlayer(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "desk-trade-test");
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, net.minecraft.server.level.ClientInformation.createDefault());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));
        player.setGameMode(GameType.SURVIVAL); return player;
    }
    private static void drops(GameTestHelper h) {
        var level=h.getLevel(); var main=h.absolutePos(new BlockPos(2,50,2)); var area=new AABB(main).inflate(5);
        var player=connectedPlayer(h);
        for(var facing:Direction.Plane.HORIZONTAL) for(boolean upper:new boolean[]{false,true})
            for(boolean creative:new boolean[]{false,true}) for(int part=0;part<(upper?4:2);part++) {
                player.setGameMode(creative?GameType.CREATIVE:GameType.SURVIVAL);
                level.getEntitiesOfClass(ItemEntity.class,area).forEach(Entity::discard);
                place(h,main,facing,upper);
                ((WritingDeskBlockEntity)level.getBlockEntity(main)).setItem(1,new ItemStack(Items.PAPER,7));
                var hit=part%2==0?main:main.relative(facing); if(part>=2)hit=hit.above();
                player.gameMode.destroyBlock(hit);
                var items=level.getEntitiesOfClass(ItemEntity.class,area).stream().map(ItemEntity::getItem).toList();
                int bases=items.stream().filter(s->s.is(MystItems.WRITING_DESK_ITEM.get())&&!ItemWritingDesk.isTopExtension(s)).mapToInt(ItemStack::getCount).sum();
                int tops=items.stream().filter(ItemWritingDesk::isTopExtension).mapToInt(ItemStack::getCount).sum();
                String context=" facing="+facing+", upper="+upper+", creative="+creative+", part="+part;
                h.assertValueEqual(bases,creative||part>=2?0:1,"one base"+context);
                h.assertValueEqual(tops,!creative&&upper?1:0,"one extension"+context);
                h.assertValueEqual(items.stream().filter(s->s.is(Items.PAPER)).mapToInt(ItemStack::getCount).sum(),part>=2?0:7,"contents drop once"+context);
                h.assertTrue(level.getBlockState(main.above()).isAir()&&level.getBlockState(main.relative(facing).above()).isAir(),"no orphan extension"+context);
                if(part<2)h.assertTrue(level.getBlockState(main).isAir()&&level.getBlockState(main.relative(facing)).isAir(),"no orphan base"+context);
                for(var p:List.of(main,main.relative(facing),main.above(),main.relative(facing).above()))level.removeBlock(p,false);
            }
        level.getEntitiesOfClass(ItemEntity.class,area).forEach(Entity::discard); h.succeed();
    }
    private static Villager villager(GameTestHelper h) {
        var v=EntityTypes.VILLAGER.create(h.getLevel(),EntitySpawnReason.TRIGGERED);
        v.setVillagerData(v.getVillagerData().withProfession(MystVillagers.ARCHIVIST)); return v;
    }
    private static void trades(GameTestHelper h) {
        var level=h.getLevel(); var fresh=villager(h); var offers=fresh.getOffers();
        h.assertValueEqual(offers.size(),4,"native data-driven stock");
        h.assertValueEqual(offers.stream().filter(o->o.getResult().is(MystItems.PAGE.get())).count(),3L,"three symbol offers");
        for(var o:offers) if(o.getResult().is(MystItems.PAGE.get())) {
            var symbol=Page.getSymbolId(o.getResult()); h.assertTrue(symbol!=null,"page retains symbol data");
            h.assertValueEqual(o.getBaseCostA().getCount(),com.xcompwiz.mystcraft.symbol.SymbolItemEconomy.archivistPrice(symbol),"rank price");
            h.assertTrue(!o.getResult().has(net.minecraft.core.component.DataComponents.ADDITIONAL_TRADE_COST),"temporary pricing component removed");
        }
        var old=villager(h); var legacy=ArchivistShopData.open(old); var symbol=Page.getSymbolId(legacy.getShopItem(0));
        old.getOffers().clear(); old.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD,25),new ItemStack(MystItems.BOOSTER.get()),7,1,.05F));
        ArchivistTrades.ensureVanillaOffers(old);
        h.assertValueEqual(old.getOffers().size(),4,"existing vanilla fallback upgraded");
        h.assertValueEqual(Page.getSymbolId(old.getOffers().get(1).getResult()),symbol,"legacy page stock preserved");
        var first=old.getOffers().getFirst(); first.increaseUses(); ArchivistTrades.ensureVanillaOffers(old);
        h.assertValueEqual(first.getUses(),1,"reopening does not replenish stock");
        old.restock(); h.assertValueEqual(first.getUses(),0,"normal workstation restock");
        var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try { MerchantOffers.STREAM_CODEC.encode(buf,old.getOffers()); h.assertValueEqual(MerchantOffers.STREAM_CODEC.decode(buf).size(),4,"native offer network sync"); }
        finally {buf.release();}
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
        old.saveWithoutId(output); var copy=villager(h);
        copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),output.buildResult()));
        h.assertValueEqual(copy.getOffers().size(),4,"offers survive full villager save");
        var player=connectedPlayer(h); player.setPos(old.position());
        old.mobInteract(player,InteractionHand.MAIN_HAND);
        h.assertTrue(player.containerMenu instanceof MerchantMenu,"ordinary MerchantMenu opens");
        var menu=(MerchantMenu)player.containerMenu; menu.setSelectionHint(0);
        menu.getSlot(0).set(new ItemStack(Items.EMERALD,20));
        h.assertTrue(menu.getSlot(2).getItem().is(MystItems.BOOSTER.get()),"native result slot offers booster");
        menu.quickMoveStack(player,2);
        h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"standard trade consumes emeralds");
        h.assertValueEqual(first.getUses(),1,"standard trade consumes stock");
        h.assertTrue(java.util.stream.IntStream.range(0,36).anyMatch(i->player.getInventory().getItem(i).is(MystItems.BOOSTER.get())),"booster delivered to inventory");
        player.closeContainer(); h.succeed();
    }
    private static void workstation(GameTestHelper h) {
        DimensionalReinforcementGameTests.retainArena(h);
        var level=h.getLevel(); var main=h.absolutePos(new BlockPos(4,30,4)); h.setTime(6000);
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)level.setBlock(main.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
        place(h,main,Direction.NORTH,false);
        h.assertTrue(MystVillagers.ARCHIVIST_POI.is(net.minecraft.tags.PoiTypeTags.ACQUIRABLE_JOB_SITE),"unemployed villagers can discover desk POI");
        h.assertTrue(level.getPoiManager().getType(main).map(t->t.is(MystVillagers.ARCHIVIST_POI.unwrapKey().orElseThrow())).orElse(false),"desk registers real POI");
        h.assertTrue(level.getPoiManager().getType(main.north()).isEmpty(),"foot is not a second workstation");
        var v=EntityTypes.VILLAGER.create(level,EntitySpawnReason.TRIGGERED); v.setPos(main.getX()+2.5,main.getY(),main.getZ()+.5);
        v.setPersistenceRequired(); level.addFreshEntity(v); v.refreshBrain(level);
        h.succeedWhen(()->{
            h.assertTrue(v.getVillagerData().profession().is(MystVillagers.ARCHIVIST.unwrapKey().orElseThrow()),"villager acquires Archivist job from placed desk");
            h.assertValueEqual(v.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElseThrow().pos(),main,"desk is the job site");
            v.discard(); h.succeed();
        });
    }
}
