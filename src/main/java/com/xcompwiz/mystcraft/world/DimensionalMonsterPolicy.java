package com.xcompwiz.mystcraft.world;

import com.xcompwiz.mystcraft.symbol.LegacySymbolId;
import com.xcompwiz.mystcraft.symbol.SymbolAvailability;
import java.util.List;

/** One opt-in risk, with a bounded, diminishing reward for additional authored pages. */
public final class DimensionalMonsterPolicy {
    public static final String SYMBOL = "mystcraft:DimensionalMonsters";
    private DimensionalMonsterPolicy() {}
    public static int pageCount(List<String> symbols) {
        if (!SymbolAvailability.isExecutable(SYMBOL)) return 0;
        return (int) symbols.stream().filter(s -> SYMBOL.equalsIgnoreCase(LegacySymbolId.qualify(s))).count();
    }
    public static int incrementalReduction(int occurrence) {
        return occurrence < 1 || occurrence > 9 ? 0 : 500 >> (occurrence - 1);
    }
    public static int reduction(List<String> symbols) {
        int count = pageCount(symbols), result = 0;
        for (int i = 1; i <= Math.min(count, 9); i++) result += incrementalReduction(i);
        return result;
    }
}
