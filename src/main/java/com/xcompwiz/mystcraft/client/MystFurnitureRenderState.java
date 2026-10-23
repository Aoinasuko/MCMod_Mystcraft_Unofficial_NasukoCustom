package com.xcompwiz.mystcraft.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Snapshot of furniture visuals; submission never reads a live block entity. */
public final class MystFurnitureRenderState extends BlockEntityRenderState {
    public BlockState state;
    public ItemStack book = ItemStack.EMPTY;
    public boolean backing;
    public int paperCount;
    public final ItemStackRenderState item = new ItemStackRenderState();
}
