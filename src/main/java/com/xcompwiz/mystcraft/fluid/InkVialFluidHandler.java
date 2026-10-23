package com.xcompwiz.mystcraft.fluid;

import com.xcompwiz.mystcraft.registry.MystFluids;
import com.xcompwiz.mystcraft.registry.MystItems;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Draining a whole vial atomically returns its glass bottle through ItemAccess. */
public final class InkVialFluidHandler extends ItemAccessResourceHandler<FluidResource> {
    public static final int CAPACITY = FluidType.BUCKET_VOLUME;
    public InkVialFluidHandler(ItemAccess access) { super(access, 1); }
    @Override protected FluidResource getResourceFrom(ItemResource item, int index) {
        return item.is(MystItems.INK_VIAL.get()) ? FluidResource.of(MystFluids.BLACK_INK.get()) : FluidResource.EMPTY;
    }
    @Override protected int getAmountFrom(ItemResource item, int index) {
        return item.is(MystItems.INK_VIAL.get()) ? CAPACITY : 0;
    }
    @Override protected ItemResource update(ItemResource item, int index, FluidResource fluid, int amount) {
        return amount == 0 ? ItemResource.of(Items.GLASS_BOTTLE) : ItemResource.EMPTY;
    }
    @Override protected int getCapacity(int index, FluidResource resource) { return CAPACITY; }
    @Override public int insert(int index, FluidResource resource, int amount, TransactionContext tx) { return 0; }
}
