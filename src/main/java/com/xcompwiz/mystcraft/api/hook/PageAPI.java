package com.xcompwiz.mystcraft.api.hook;
import net.minecraft.resources.Identifier; import net.minecraft.world.item.ItemStack; import java.util.Collection;
public interface PageAPI { boolean hasLinkPanel(ItemStack page); Collection<String> getPageLinkProperties(ItemStack page); boolean isPageWritable(ItemStack page); Identifier getPageSymbol(ItemStack page); void setPageSymbol(ItemStack page,Identifier symbol); }
