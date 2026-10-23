package com.xcompwiz.mystcraft.registry;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.entity.EntityLinkbook;
import com.xcompwiz.mystcraft.entity.EntityMeteor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Physical books, meteors and NasukoCustom dimensional monsters. */
public final class MystEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Mystcraft.MOD_ID);

    public static final Supplier<EntityType<EntityLinkbook>> LINKBOOK = ENTITY_TYPES.register(
            "myst.book",
            () -> EntityType.Builder.<EntityLinkbook>of(EntityLinkbook::new, MobCategory.MISC)
                    .sized(0.25F, 0.20F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.parse(Mystcraft.MOD_ID + ":myst.book"))));

    public static final Supplier<EntityType<EntityMeteor>> METEOR = ENTITY_TYPES.register(
            "myst.meteor",
            () -> EntityType.Builder.<EntityMeteor>of(EntityMeteor::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(192)
                    .updateInterval(2)
                    .build(net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.parse(Mystcraft.MOD_ID + ":myst.meteor"))));

    public static final Supplier<EntityType<com.xcompwiz.mystcraft.entity.monster.DimensionalPterosaur>> DIMENSIONAL_PTEROSAUR = ENTITY_TYPES.register(
            "dimensional_pterosaur", () -> EntityType.Builder.of(com.xcompwiz.mystcraft.entity.monster.DimensionalPterosaur::new, MobCategory.MONSTER)
                    .sized(1.3F,1.0F).clientTrackingRange(10).updateInterval(2)
                    .build(net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.parse("mystcraft:dimensional_pterosaur"))));
    public static final Supplier<EntityType<com.xcompwiz.mystcraft.entity.monster.DimensionalSerpent>> DIMENSIONAL_SERPENT = ENTITY_TYPES.register(
            "dimensional_serpent", () -> EntityType.Builder.of(com.xcompwiz.mystcraft.entity.monster.DimensionalSerpent::new, MobCategory.MONSTER)
                    .sized(.7F,.45F).clientTrackingRange(10).updateInterval(2)
                    .build(net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.parse("mystcraft:dimensional_serpent"))));

    public static final Supplier<EntityType<com.xcompwiz.mystcraft.entity.monster.DimensionalFairy>> DIMENSIONAL_FAIRY = monster("dimensional_fairy", com.xcompwiz.mystcraft.entity.monster.DimensionalFairy::new, .6F, 1.05F);
    public static final Supplier<EntityType<com.xcompwiz.mystcraft.entity.monster.DimensionalWorm>> DIMENSIONAL_WORM = monster("dimensional_worm", com.xcompwiz.mystcraft.entity.monster.DimensionalWorm::new, 1.8F, 1.5F);
    public static final Supplier<EntityType<com.xcompwiz.mystcraft.entity.monster.DimensionalCalamity>> DIMENSIONAL_CALAMITY = monster("dimensional_calamity", com.xcompwiz.mystcraft.entity.monster.DimensionalCalamity::new, 1.4F, 3F);
    public static final Supplier<EntityType<com.xcompwiz.mystcraft.entity.monster.CalamityOrb>> CALAMITY_ORB = ENTITY_TYPES.register("calamity_orb", () -> EntityType.Builder.of(com.xcompwiz.mystcraft.entity.monster.CalamityOrb::new, MobCategory.MISC)
            .sized(.5F, .5F).clientTrackingRange(10).updateInterval(1).build(key("calamity_orb")));
    public static final Supplier<EntityType<com.xcompwiz.mystcraft.entity.monster.DimensionalLaser>> DIMENSIONAL_LASER = ENTITY_TYPES.register("dimensional_laser", () -> EntityType.Builder.of(com.xcompwiz.mystcraft.entity.monster.DimensionalLaser::new, MobCategory.MISC)
            .sized(.1F, .1F).noSave().clientTrackingRange(10).updateInterval(1).build(key("dimensional_laser")));
    public static final Supplier<EntityType<com.xcompwiz.mystcraft.entity.monster.DimensionalSummoningCircle>> SUMMONING_CIRCLE = ENTITY_TYPES.register("dimensional_summoning_circle", () -> EntityType.Builder.of(com.xcompwiz.mystcraft.entity.monster.DimensionalSummoningCircle::new, MobCategory.MISC)
            .sized(2.2F, .1F).noSave().clientTrackingRange(10).updateInterval(1).build(key("dimensional_summoning_circle")));
    private static net.minecraft.resources.ResourceKey<EntityType<?>> key(String name) {
        return net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("mystcraft", name));
    }
    private static <T extends net.minecraft.world.entity.Mob> Supplier<EntityType<T>> monster(String name, EntityType.EntityFactory<T> factory, float width, float height) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MONSTER).sized(width, height)
                .clientTrackingRange(10).updateInterval(2).build(key(name)));
    }

    private MystEntities() {}

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
        bus.addListener(com.xcompwiz.mystcraft.entity.monster.DimensionalMonsterSpawns::attributes);
        bus.addListener(com.xcompwiz.mystcraft.entity.monster.DimensionalMonsterSpawns::placements);
    }
}
