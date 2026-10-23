package com.xcompwiz.mystcraft.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

/** Type checks for the existing Mystcraft book and Age NBT schema. */
public final class LegacyNbt {
    private LegacyNbt() {}

    public static boolean contains(CompoundTag compound, String key, int type) {
        Tag value = compound.get(key);
        return value != null && (type == 99 ? value instanceof NumericTag : value.getId() == type);
    }

    public static ItemStack readItem(HolderLookup.Provider registries, CompoundTag tag) {
        return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                .result().orElse(ItemStack.EMPTY);
    }

    public static Tag writeItem(ItemStack stack, HolderLookup.Provider registries) {
        return ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
                .getOrThrow();
    }
}
