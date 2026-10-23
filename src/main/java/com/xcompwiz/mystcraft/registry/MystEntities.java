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

    private MystEntities() {}

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
        bus.addListener(com.xcompwiz.mystcraft.entity.monster.DimensionalMonsterSpawns::attributes);
        bus.addListener(com.xcompwiz.mystcraft.entity.monster.DimensionalMonsterSpawns::placements);
    }
}
