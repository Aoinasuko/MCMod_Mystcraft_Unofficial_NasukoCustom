package com.xcompwiz.mystcraft.api.hook;
import com.xcompwiz.mystcraft.api.client.ILinkPanelEffect; import com.xcompwiz.mystcraft.api.util.Color; import net.minecraft.resources.Identifier;
public interface RenderAPI { void registerRenderEffect(ILinkPanelEffect renderer); void drawWord(float x,float y,float zLevel,float scale,String word); void drawSymbol(float x,float y,float zLevel,float scale,Identifier identifier); void drawColorEye(float x,float y,float zLevel,float radius,Color color); }
