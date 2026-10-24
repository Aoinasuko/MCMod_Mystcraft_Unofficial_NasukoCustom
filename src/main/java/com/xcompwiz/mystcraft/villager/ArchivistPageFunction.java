package com.xcompwiz.mystcraft.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.symbol.SymbolItemEconomy;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native 26.3 trade modifier: a symbol page and its rank-dependent emerald price. */
public final class ArchivistPageFunction extends LootItemConditionalFunction {
    public static final MapCodec<ArchivistPageFunction> CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i)
            .and(Codec.intRange(1, 3).fieldOf("min_rank").forGetter(f -> f.minRank)).apply(i, ArchivistPageFunction::new));
    private static final DeferredRegister<MapCodec<? extends LootItemFunction>> TYPES = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, "mystcraft");
    static { TYPES.register("archivist_page", () -> CODEC); }
    private final int minRank;
    private ArchivistPageFunction(Optional<Holder<LootItemCondition>> condition, int minRank) { super(condition); this.minRank = minRank; }
    public static void register(IEventBus bus) { TYPES.register(bus); }
    @Override public MapCodec<ArchivistPageFunction> codec() { return CODEC; }
    @Override public ItemStack run(ItemStack stack, LootContext context) {
        var symbol = SymbolItemEconomy.chooseAtLeastRank(context.getRandom(), minRank);
        if (symbol == null) return ItemStack.EMPTY;
        var page = Page.createSymbolPage(symbol.legacyId());
        page.set(DataComponents.ADDITIONAL_TRADE_COST, SymbolItemEconomy.archivistPrice(symbol.legacyId()) - 4);
        return page;
    }
}
