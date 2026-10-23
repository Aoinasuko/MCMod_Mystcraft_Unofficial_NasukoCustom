package com.xcompwiz.mystcraft.registry;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Consumer;

/**
 * Black Ink fluid restored from Mystcraft 0.13.7.06.
 *
 * <p>The legacy Forge fluid registry id was {@code myst.ink.black}; the fluid block
 * remains {@code mystcraft:fluidblockblackink}.  The old Ink Vial represents one
 * bucket-volume (1000 mB) charge of this fluid.</p>
 */
public final class MystFluids {
    public static final String LEGACY_BLACK_INK_NAME = "myst.ink.black";

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Mystcraft.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, Mystcraft.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> BLACK_INK_TYPE = FLUID_TYPES.register(
            LEGACY_BLACK_INK_NAME,
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.mystcraft.black_ink")
                    .density(1000)
                    .viscosity(1000)));

    public static final DeferredHolder<Fluid, FlowingFluid> BLACK_INK = FLUIDS.register(
            LEGACY_BLACK_INK_NAME,
            () -> new BaseFlowingFluid.Source(properties()));

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_BLACK_INK = FLUIDS.register(
            LEGACY_BLACK_INK_NAME + "_flowing",
            () -> new BaseFlowingFluid.Flowing(properties()));

    private MystFluids() {}

    private static BaseFlowingFluid.Properties properties() {
        // 0.13.7.06 exposed the fluid itself to Forge fluid handlers.  It did not
        // have a dedicated Mystcraft bucket item; the vial is handled separately as
        // a fixed 1000 mB capability container, so no modern BucketItem is invented.
        return new BaseFlowingFluid.Properties(BLACK_INK_TYPE, BLACK_INK, FLOWING_BLACK_INK)
                .block(MystBlocks.BLACK_INK);
    }

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
    }
}
