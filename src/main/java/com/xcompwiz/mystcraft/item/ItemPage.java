package com.xcompwiz.mystcraft.item;

import com.xcompwiz.mystcraft.client.LinkPropertyName;

import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.api.item.IItemRenameable;
import com.xcompwiz.mystcraft.api.item.IItemWritable;
import com.xcompwiz.mystcraft.api.item.IItemPageProvider;
import net.minecraft.resources.Identifier;
import com.xcompwiz.mystcraft.client.LegacySymbolName;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.symbol.SymbolRemappings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;

import java.util.Collection;
import java.util.List;

public final class ItemPage extends Item implements IItemRenameable, IItemWritable, IItemPageProvider {
    public ItemPage(Properties properties) {
        super(properties);
    }

    @Override
    public String getDisplayName(Player player, ItemStack stack) { return getName(stack).getString(); }

    @Override
    public void setDisplayName(Player player, ItemStack stack, String name) { /* Legacy ItemPage rename was intentionally a no-op. */ }

    @Override
    public boolean writeSymbol(Player player, ItemStack stack, Identifier symbol) {
        if (!Page.isBlank(stack) || symbol == null) return false;
        Page.setSymbolId(stack, symbol.toString());
        return true;
    }

    @Override
    public List<ItemStack> getPageList(Player player, ItemStack stack) { return List.of(stack); }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot equipmentSlot) {
        super.inventoryTick(stack, level, entity, equipmentSlot);
        if (level.isClientSide() || !(entity instanceof Player player) || stack.isEmpty()) return;
        if (Page.isBlank(stack) || Page.isLinkPanel(stack)) return;

        int slotId = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i) == stack) { slotId = i; break; }
        }
        if (slotId < 0) return;
        List<ItemStack> mapping = SymbolRemappings.remapPage(stack);
        if (mapping.isEmpty()) {
            stack.setCount(0);
            return;
        }
        if (mapping.size() == 1) {
            ItemStack mapped = mapping.getFirst();
            if (!ItemStack.isSameItemSameComponents(stack, mapped)) {
                player.getInventory().setItem(slotId, mapped);
            }
            return;
        }

        ItemStack folder = new ItemStack(MystItems.FOLDER.get());
        if (folder.getItem() instanceof ItemPageContainer container) {
            container.setPages(folder, mapping);
            player.getInventory().setItem(slotId, folder);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        if (Page.isLinkPanel(stack)) {
            return Component.translatable(getDescriptionId() + ".panel");
        }
        if (Page.isBlank(stack)) {
            return Component.translatable(getDescriptionId() + ".blank");
        }
        String symbol = Page.getSymbolId(stack);
        if (symbol == null) {
            return Component.translatable(getDescriptionId() + ".symbol", "Unknown");
        }
        return Component.translatable(getDescriptionId() + ".symbol", LegacySymbolName.component(symbol));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        Collection<String> properties = Page.getLinkProperties(stack);
        if (properties != null) {
            for (String property : properties) {
                tooltipComponents.accept(Component.translatable("tooltip.mystcraft.link_property", LinkPropertyName.component(property))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        // 1.21 creative name search is built from tooltip lines on Worker-Main threads.
        // Feed the canonical English symbol name to that index only; the normal render-thread
        // tooltip remains Japanese-only when the client language is Japanese.
        String symbol = Page.getSymbolId(stack);
        if (symbol != null && Thread.currentThread().getName().startsWith("Worker-Main")) {
            tooltipComponents.accept(Component.literal(LegacySymbolName.englishName(symbol)));
        }
        if (com.xcompwiz.mystcraft.world.DimensionalMonsterPolicy.SYMBOL.equalsIgnoreCase(symbol)) {
            tooltipComponents.accept(Component.translatable("tooltip.mystcraft.dimensional_monsters.spawning").withStyle(ChatFormatting.DARK_PURPLE));
            tooltipComponents.accept(Component.translatable("tooltip.mystcraft.dimensional_monsters.stability").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, display, tooltipComponents, tooltipFlag);
    }
}
