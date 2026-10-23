package com.xcompwiz.mystcraft.api.hook;
import com.xcompwiz.mystcraft.api.symbol.IAgeSymbol; import net.minecraft.resources.Identifier; import java.util.List;
public interface SymbolAPI { void blacklistIdentifier(Identifier identifier); List<IAgeSymbol> getAllRegisteredSymbols(); IAgeSymbol getSymbol(Identifier identifier); String getSymbolOwner(Identifier identifier); }
