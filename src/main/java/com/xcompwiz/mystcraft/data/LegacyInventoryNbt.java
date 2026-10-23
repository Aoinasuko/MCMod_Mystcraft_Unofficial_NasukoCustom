package com.xcompwiz.mystcraft.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;

public final class LegacyInventoryNbt {
    private LegacyInventoryNbt() {}

    public static void save(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        ContainerHelper.saveAllItems(output, items);
        tag.merge(output.buildResult());
    }
}
