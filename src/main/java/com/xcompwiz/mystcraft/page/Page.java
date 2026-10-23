package com.xcompwiz.mystcraft.page;

import com.xcompwiz.mystcraft.linking.LinkOptions;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.symbol.LegacySymbolId;
import com.xcompwiz.mystcraft.util.LegacyItemData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 1.21.1 implementation of the legacy Mystcraft page payload.
 *
 * <p>The keys deliberately match 0.13.7.06: {@code Quality},
 * {@code linkpanel}, {@code properties}, and {@code symbol}.</p>
 */
public final class Page {
    public static final String KEY_QUALITY = "Quality";
    public static final String KEY_LINK_PANEL = "linkpanel";
    public static final String KEY_PROPERTIES = "properties";
    public static final String KEY_SYMBOL = "symbol";

    private Page() {}

    public static void setQuality(ItemStack page, String trait, int quality) {
        if (page.isEmpty()) return;
        LegacyItemData.update(page, data -> {
            CompoundTag qualities = com.xcompwiz.mystcraft.data.LegacyNbt.contains(data, KEY_QUALITY, Tag.TAG_COMPOUND)
                    ? data.getCompoundOrEmpty(KEY_QUALITY)
                    : new CompoundTag();
            qualities.putInt(trait, quality);
            data.put(KEY_QUALITY, qualities);
        });
    }

    public static int getTotalQuality(ItemStack page) {
        CompoundTag data = LegacyItemData.copy(page);
        if (!com.xcompwiz.mystcraft.data.LegacyNbt.contains(data, KEY_QUALITY, Tag.TAG_COMPOUND)) return 0;
        CompoundTag qualities = data.getCompoundOrEmpty(KEY_QUALITY);
        int total = 0;
        for (String key : qualities.keySet()) {
            total += qualities.getInt(key).orElse(0);
        }
        return total;
    }

    public static Integer getQuality(ItemStack page, String trait) {
        CompoundTag data = LegacyItemData.copy(page);
        if (!com.xcompwiz.mystcraft.data.LegacyNbt.contains(data, KEY_QUALITY, Tag.TAG_COMPOUND)) return null;
        CompoundTag qualities = data.getCompoundOrEmpty(KEY_QUALITY);
        return com.xcompwiz.mystcraft.data.LegacyNbt.contains(qualities, trait, Tag.TAG_INT) ? qualities.getInt(trait).orElse(0) : null;
    }

    public static boolean isBlank(ItemStack page) {
        if (page.isEmpty()) return true;
        CompoundTag data = LegacyItemData.copy(page);
        return !com.xcompwiz.mystcraft.data.LegacyNbt.contains(data, KEY_LINK_PANEL, Tag.TAG_COMPOUND) && !com.xcompwiz.mystcraft.data.LegacyNbt.contains(data, KEY_SYMBOL, Tag.TAG_STRING);
    }

    public static boolean isLinkPanel(ItemStack page) {
        if (page.isEmpty()) return false;
        return com.xcompwiz.mystcraft.data.LegacyNbt.contains(LegacyItemData.copy(page), KEY_LINK_PANEL, Tag.TAG_COMPOUND);
    }

    public static void makeLinkPanel(ItemStack page) {
        if (page.isEmpty()) return;
        LegacyItemData.update(page, data -> {
            if (!com.xcompwiz.mystcraft.data.LegacyNbt.contains(data, KEY_LINK_PANEL, Tag.TAG_COMPOUND)) {
                data.put(KEY_LINK_PANEL, new CompoundTag());
            }
        });
    }

    public static void addLinkProperty(ItemStack page, String linkProperty) {
        if (page.isEmpty()) return;
        LegacyItemData.update(page, data -> {
            CompoundTag panel = com.xcompwiz.mystcraft.data.LegacyNbt.contains(data, KEY_LINK_PANEL, Tag.TAG_COMPOUND)
                    ? data.getCompoundOrEmpty(KEY_LINK_PANEL)
                    : new CompoundTag();
            ListTag properties = panel.getListOrEmpty(KEY_PROPERTIES);
            properties.add(StringTag.valueOf(linkProperty));
            panel.put(KEY_PROPERTIES, properties);
            data.put(KEY_LINK_PANEL, panel);
        });
    }

    public static Collection<String> getLinkProperties(ItemStack page) {
        if (page.isEmpty()) return null;
        CompoundTag data = LegacyItemData.copy(page);
        if (!com.xcompwiz.mystcraft.data.LegacyNbt.contains(data, KEY_LINK_PANEL, Tag.TAG_COMPOUND)) return null;

        CompoundTag panel = data.getCompoundOrEmpty(KEY_LINK_PANEL);
        ListTag properties = panel.getListOrEmpty(KEY_PROPERTIES);
        List<String> result = new ArrayList<>(properties.size());
        for (int i = 0; i < properties.size(); i++) {
            result.add(properties.getString(i).orElse(""));
        }
        return result;
    }

    public static void applyLinkPanel(ItemStack linkPanel, ItemStack linkingItem) {
        Collection<String> properties = getLinkProperties(linkPanel);
        if (properties == null) return;
        for (String property : properties) {
            LinkOptions.setFlag(linkingItem, property, true);
        }
    }

    public static void setSymbol(ItemStack page, Identifier symbol) {
        setSymbolId(page, symbol == null ? null : symbol.toString());
    }

    /** Stores the exact legacy symbol string, including mixed-case 1.12 paths. */
    public static void setSymbolId(ItemStack page, String symbolId) {
        if (page.isEmpty()) return;
        LegacyItemData.update(page, data -> {
            if (symbolId == null || symbolId.isBlank()) {
                data.remove(KEY_SYMBOL);
            } else {
                data.putString(KEY_SYMBOL, LegacySymbolId.qualify(symbolId));
            }
        });
    }

    /** Returns the exact persisted legacy symbol identity. */
    public static String getSymbolId(ItemStack page) {
        if (page.isEmpty()) return null;
        CompoundTag data = LegacyItemData.copy(page);
        if (!com.xcompwiz.mystcraft.data.LegacyNbt.contains(data, KEY_SYMBOL, Tag.TAG_STRING)) return null;
        String id = data.getString(KEY_SYMBOL).orElse("");
        return id.isBlank() ? null : LegacySymbolId.qualify(id);
    }

    /**
     * Returns the port-side canonical key for code that requires a modern
     * Identifier. Do not write this key back into legacy NBT.
     */
    public static Identifier getSymbol(ItemStack page) {
        return LegacySymbolId.canonicalKey(getSymbolId(page));
    }

    public static ItemStack createPage() {
        ItemStack page = new ItemStack(MystItems.PAGE.get());
        LegacyItemData.set(page, new CompoundTag());
        return page;
    }

    public static ItemStack createLinkPage() {
        ItemStack page = createPage();
        makeLinkPanel(page);
        return page;
    }

    public static ItemStack createLinkPage(String property) {
        ItemStack page = createPage();
        addLinkProperty(page, property);
        return page;
    }

    public static ItemStack createSymbolPage(Identifier symbol) {
        ItemStack page = createPage();
        setSymbol(page, symbol);
        return page;
    }

    public static ItemStack createSymbolPage(String legacySymbolId) {
        ItemStack page = createPage();
        setSymbolId(page, legacySymbolId);
        return page;
    }

    public static List<String> copyLinkProperties(ItemStack page) {
        Collection<String> properties = getLinkProperties(page);
        return properties == null ? List.of() : List.copyOf(properties);
    }
}
