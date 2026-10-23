package com.xcompwiz.mystcraft.api.hook;
import com.xcompwiz.mystcraft.api.symbol.IAgeSymbol; import net.minecraft.resources.Identifier; import java.util.*;
public interface GrammarAPI { void registerGrammarRule(Identifier parent,Integer rank,Identifier... args); Collection<IAgeSymbol> getSymbolsExpandingToken(Identifier token); Collection<Identifier> getTokensProducingToken(Identifier token); List<Identifier> generateFromToken(Identifier root,Random rand); List<Identifier> generateFromToken(Identifier root,Random rand,List<Identifier> written); }
