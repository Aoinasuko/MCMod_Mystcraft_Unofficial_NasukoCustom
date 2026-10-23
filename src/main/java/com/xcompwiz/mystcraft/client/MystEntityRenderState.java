package com.xcompwiz.mystcraft.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.item.ItemStack;

public final class MystEntityRenderState extends EntityRenderState {
    public ItemStack book = ItemStack.EMPTY;
    public float yaw;
    public float pitch;
    public float scale;
}
