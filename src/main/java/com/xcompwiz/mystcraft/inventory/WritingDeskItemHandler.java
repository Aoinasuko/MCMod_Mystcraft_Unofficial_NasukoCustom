package com.xcompwiz.mystcraft.inventory;

import com.xcompwiz.mystcraft.blockentity.WritingDeskBlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Transactional automation view preserving the legacy input and output slot policy. */
public final class WritingDeskItemHandler implements ResourceHandler<ItemResource> {
    private final WritingDeskBlockEntity desk;
    private final ResourceHandler<ItemResource> storage;

    public WritingDeskItemHandler(WritingDeskBlockEntity desk) {
        this.desk = desk;
        this.storage = VanillaContainerWrapper.of(desk);
    }

    @Override public int size() { return WritingDeskBlockEntity.WORK_SLOT_COUNT; }
    @Override public ItemResource getResource(int index) { return storage.getResource(index); }
    @Override public long getAmountAsLong(int index) { return storage.getAmountAsLong(index); }
    @Override public long getCapacityAsLong(int index, ItemResource resource) {
        return isValid(index, resource) || resource.isEmpty() ? storage.getCapacityAsLong(index, resource) : 0;
    }
    @Override public boolean isValid(int index, ItemResource resource) {
        return index == WritingDeskBlockEntity.PAPER_SLOT && desk.canPlaceItem(index, resource.toStack());
    }
    @Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return isValid(index, resource) ? storage.insert(index, resource, amount, transaction) : 0;
    }
    @Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return 0;
    }
}
