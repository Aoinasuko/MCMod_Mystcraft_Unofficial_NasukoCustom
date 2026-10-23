package com.xcompwiz.mystcraft.fluid;

import com.xcompwiz.mystcraft.blockentity.WritingDeskBlockEntity;
import com.xcompwiz.mystcraft.registry.MystFluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Transactional external view of the Writing Desk inkwell. */
public final class WritingDeskInkHandler extends SnapshotJournal<Integer> implements ResourceHandler<FluidResource> {
    private final WritingDeskBlockEntity desk;
    public WritingDeskInkHandler(WritingDeskBlockEntity desk) { this.desk = desk; }
    private static void checkIndex(int index) { java.util.Objects.checkIndex(index, 1); }
    @Override public int size() { return 1; }
    @Override public FluidResource getResource(int index) {
        checkIndex(index);
        return desk.inkAmount() == 0 ? FluidResource.EMPTY : FluidResource.of(MystFluids.BLACK_INK.get());
    }
    @Override public long getAmountAsLong(int index) { checkIndex(index); return desk.inkAmount(); }
    @Override public boolean isValid(int index, FluidResource resource) {
        checkIndex(index);
        return resource.is(MystFluids.BLACK_INK.get());
    }
    @Override public long getCapacityAsLong(int index, FluidResource resource) {
        checkIndex(index);
        return resource.isEmpty() || isValid(index, resource) ? WritingDeskBlockEntity.CAPACITY : 0;
    }
    @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        checkIndex(index);
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!isValid(index, resource)) return 0;
        int accepted = Math.min(amount, WritingDeskBlockEntity.CAPACITY - desk.inkAmount());
        if (accepted > 0) {
            updateSnapshots(transaction);
            desk.setInkAmountInTransaction(desk.inkAmount() + accepted);
        }
        return accepted;
    }
    @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        checkIndex(index);
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!isValid(index, resource)) return 0;
        int extracted = Math.min(amount, desk.inkAmount());
        if (extracted > 0) {
            updateSnapshots(transaction);
            desk.setInkAmountInTransaction(desk.inkAmount() - extracted);
        }
        return extracted;
    }
    @Override protected Integer createSnapshot() { return desk.inkAmount(); }
    @Override protected void revertToSnapshot(Integer amount) { desk.setInkAmountInTransaction(amount); }
    @Override protected void onRootCommit(Integer previous) { desk.setChanged(); }
}
