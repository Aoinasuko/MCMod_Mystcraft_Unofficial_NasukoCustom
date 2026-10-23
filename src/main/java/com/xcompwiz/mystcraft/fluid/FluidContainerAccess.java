package com.xcompwiz.mystcraft.fluid;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Isolated single-container workspace; the caller commits its result to inventory only if it fits. */
public final class FluidContainerAccess {
    public enum FluidAction { SIMULATE, EXECUTE }
    private final ItemStacksResourceHandler items;
    private final ResourceHandler<FluidResource> fluids;

    private FluidContainerAccess(ItemStacksResourceHandler items, ResourceHandler<FluidResource> fluids) {
        this.items = items;
        this.fluids = fluids;
    }

    public static FluidContainerAccess of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        var items = new ItemStacksResourceHandler(1);
        items.set(0, ItemResource.of(stack), 1);
        var fluids = ItemAccess.forHandlerIndexStrict(items, 0).getCapability(Capabilities.Fluid.ITEM);
        return fluids == null ? null : new FluidContainerAccess(items, fluids);
    }

    public int getTanks() { return fluids.size(); }
    public FluidStack getFluidInTank(int index) {
        return fluids.getResource(index).toStack(fluids.getAmountAsInt(index));
    }
    public ItemStack getContainer() { return items.getResource(0).toStack(items.getAmountAsInt(0)); }
    public FluidStack drain(FluidStack request, FluidAction action) {
        if (request.isEmpty()) return FluidStack.EMPTY;
        try (var transaction = Transaction.openRoot()) {
            var resource = FluidResource.of(request);
            int extracted = fluids.extract(resource, request.getAmount(), transaction);
            if (action == FluidAction.EXECUTE) transaction.commit();
            return resource.toStack(extracted);
        }
    }
    public int fill(FluidStack request, FluidAction action) {
        if (request.isEmpty()) return 0;
        try (var transaction = Transaction.openRoot()) {
            int inserted = fluids.insert(FluidResource.of(request), request.getAmount(), transaction);
            if (action == FluidAction.EXECUTE) transaction.commit();
            return inserted;
        }
    }
}
