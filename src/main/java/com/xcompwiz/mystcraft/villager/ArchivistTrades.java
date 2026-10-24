package com.xcompwiz.mystcraft.villager;

import com.xcompwiz.mystcraft.registry.MystItems;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;

/** One-time conversion of the old custom shop into native, persisted/restockable offers. */
public final class ArchivistTrades {
    private static final String VERSION = "NasukoVanillaTrades";
    private ArchivistTrades() {}
    public static void ensureVanillaOffers(Villager villager) {
        if (villager.level().isClientSide() || villager.getPersistentData().getIntOr(VERSION, 0) >= 1) return;
        var offers = villager.getOffers();
        boolean legacyStock = villager.getPersistentData().getCompoundOrEmpty("Mystcraft").contains("Trade");
        if (legacyStock || offers.stream().noneMatch(o -> o.getResult().is(MystItems.PAGE.get()))) {
            var shop = ArchivistShopData.open(villager);
            var converted = new MerchantOffers();
            converted.add(offer(new ItemStack(MystItems.BOOSTER.get()), shop.getBoosterCost(), shop.getBoosterCount(), 5));
            for (int i = 0; i < 3; i++) {
                var stock = shop.getShopItem(i);
                if (!stock.isEmpty()) converted.add(offer(stock.copyWithCount(1), shop.getShopItemPrice(i), stock.getCount(), 3));
            }
            // AbstractVillager.overrideOffers is a no-op on the server; mutate its actual offer list.
            offers.clear(); offers.addAll(converted);
        }
        villager.getPersistentData().putInt(VERSION, 1);
    }
    private static MerchantOffer offer(ItemStack result, int price, int remaining, int normalStock) {
        int max = Math.max(normalStock, remaining);
        return new MerchantOffer(new ItemCost(Items.EMERALD, Math.clamp(price, 1, 64)), java.util.Optional.empty(),
                result, max - Math.max(0, remaining), max, 1, .05F);
    }
}
