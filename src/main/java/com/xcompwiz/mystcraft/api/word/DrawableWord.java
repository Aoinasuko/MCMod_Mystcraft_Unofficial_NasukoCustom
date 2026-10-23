package com.xcompwiz.mystcraft.api.word;
import net.minecraft.resources.Identifier; import java.util.*;
public final class DrawableWord {
    private final ArrayList<Integer> components=new ArrayList<>(), colors=new ArrayList<>(); private Identifier imageSource;
    public static final Identifier word_components=Identifier.fromNamespaceAndPath("mystcraft","textures/symbolcomponents.png");
    public DrawableWord(){} public DrawableWord(Integer[] components){this.components.addAll(Arrays.asList(components));}
    public ArrayList<Integer> components(){return components;} public ArrayList<Integer> colors(){return colors;}
    public DrawableWord addDrawComponent(int slot,int color){components.add(slot);colors.add(color);return this;}
    public DrawableWord addDrawComponent(int x,int y,int color){return addDrawComponent(x+y*8,color);}
    public DrawableWord addDrawComponents(int[] comps,int color){for(int c:comps)addDrawComponent(c,color);return this;}
    public DrawableWord addDrawComponents(int[] comps,int[] cols){int def=cols.length>0?cols[0]:0;for(int i=0;i<comps.length;i++)addDrawComponent(comps[i],cols.length>i?cols[i]:def);return this;}
    public DrawableWord addDrawWord(int[][] word){if(word.length==2)addDrawComponents(word[0],word[1]);return this;}
    public Identifier imageSource(){return imageSource!=null?imageSource:word_components;} public DrawableWord setImageSource(Identifier source){imageSource=source;return this;}
}
