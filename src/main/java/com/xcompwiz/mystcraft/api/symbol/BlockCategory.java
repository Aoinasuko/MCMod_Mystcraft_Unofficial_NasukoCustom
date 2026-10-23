package com.xcompwiz.mystcraft.api.symbol;
import net.minecraft.resources.Identifier;
import java.util.Collection; import java.util.LinkedHashMap; import java.util.Map; import java.util.List;
public final class BlockCategory {
    private static final Map<Identifier,BlockCategory> CATEGORIES=new LinkedHashMap<>();
    public static final BlockCategory ANY=registerBlockCategory(Identifier.fromNamespaceAndPath("mystcraft","BlockAny"));
    public static final BlockCategory SOLID=registerBlockCategory(Identifier.fromNamespaceAndPath("mystcraft","BlockSolid"));
    public static final BlockCategory FLUID=registerBlockCategory(Identifier.fromNamespaceAndPath("mystcraft","BlockFluid"));
    public static final BlockCategory GAS=registerBlockCategory(Identifier.fromNamespaceAndPath("mystcraft","BlockGas"));
    public static final BlockCategory TERRAIN=registerBlockCategory(Identifier.fromNamespaceAndPath("mystcraft","BlockTerrain"));
    public static final BlockCategory STRUCTURE=registerBlockCategory(Identifier.fromNamespaceAndPath("mystcraft","BlockStructure"));
    public static final BlockCategory ORGANIC=registerBlockCategory(Identifier.fromNamespaceAndPath("mystcraft","BlockOrganic"));
    public static final BlockCategory CRYSTAL=registerBlockCategory(Identifier.fromNamespaceAndPath("mystcraft","BlockCrystal"));
    public static final BlockCategory SEA=registerBlockCategory(Identifier.fromNamespaceAndPath("mystcraft","BlockSea"));
    private final Identifier name; private BlockCategory(Identifier n){name=n;}
    public static synchronized BlockCategory registerBlockCategory(Identifier n){return CATEGORIES.computeIfAbsent(n,BlockCategory::new);} public static BlockCategory getBlockCategory(Identifier n){return CATEGORIES.get(n);} public static Collection<BlockCategory> getCategories(){return List.copyOf(CATEGORIES.values());}
    public Identifier getName(){return name;} public Identifier getGrammarBinding(){return name;}
}
