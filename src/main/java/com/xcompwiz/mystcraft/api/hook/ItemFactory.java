package com.xcompwiz.mystcraft.api.hook;
import net.minecraft.resources.Identifier; import net.minecraft.world.item.ItemStack;
public interface ItemFactory { ItemStack buildPage(); ItemStack buildSymbolPage(Identifier identifier); ItemStack buildLinkPage(String... properties); ItemStack buildCollectionItem(String name,Identifier... tokens); ItemStack buildCollectionItem(String name,ItemStack... pages); }
