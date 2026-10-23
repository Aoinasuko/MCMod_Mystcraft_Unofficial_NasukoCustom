package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.inventory.InkMixerMenu;
import com.xcompwiz.mystcraft.linking.LinkProperties;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.21.1 rendering bridge for the legacy 176x181 Ink Mixer GUI. */
public final class InkMixerScreen extends AbstractContainerScreen<InkMixerMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "gui/inkmixer.png");
    private static final int BASIN_X = 88;
    private static final int BASIN_Y = 49;
    private static final int BASIN_RADIUS_SQ = 900;
    private int gradientFrame;

    public InkMixerScreen(InkMixerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 181);
        inventoryLabelX = 8;
        inventoryLabelY = 87;
        titleLabelY = 5;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Legacy first painted the basin backing from the unused texture region.
        LegacyGuiDrawing.blit(graphics, TEXTURE, leftPos + 54, topPos + 16, 179, 16, 66, 65, 256, 256);
        if (menu.hasInk()) {
            // 0.13.7.06 first rendered Black Ink, then animated the catalyst-property
            // gradient at frame/300F with alpha 0x40 at the top and 0xB0 at the bottom.
            // Keep the same temporal/color math while using a compact solid Black Ink
            // base instead of the old Forge fluid-atlas helper.
            graphics.fill(leftPos + 54, topPos + 16, leftPos + 120, topPos + 81, 0xFF191919);
            gradientFrame++;
            int rgb = menu.getLegacyGradientColor(gradientFrame / 300.0F);
            int top = 0x40000000 | rgb;
            int bottom = 0xB0000000 | rgb;
            graphics.fillGradient(leftPos + 54, topPos + 16, leftPos + 120, topPos + 81, top, bottom);
        }
        LegacyGuiDrawing.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        double x = mouseX - leftPos - BASIN_X;
        double y = mouseY - topPos - BASIN_Y;
        if (x * x + y * y < BASIN_RADIUS_SQ && !menu.getCarried().isEmpty()) {
            if (minecraft != null && minecraft.gameMode != null) {
                int action = button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT ? InkMixerMenu.ACTION_CATALYST_SINGLE : InkMixerMenu.ACTION_CATALYST_ALL;
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Legacy GuiInkMixer drew only the player inventory label; the mixer itself
        // had no GUI title text. Avoid AbstractContainerScreen's default duplicate.
        graphics.text(font, Component.translatable("container.inventory"), 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractTooltip(graphics, mouseX, mouseY);

        int dx = mouseX - leftPos - BASIN_X;
        int dy = mouseY - topPos - BASIN_Y;
        if (dx * dx + dy * dy < BASIN_RADIUS_SQ && menu.hasInk()) {
            java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
            lines.add(Component.translatable("gui.mystcraft.ink_mixer.basin"));
            for (String property : LinkProperties.LEGACY_INK_PROPERTIES) {
                float chance = menu.getPropertyProbability(property);
                if (chance > 0.0001F) {
                    lines.add(Component.literal(LinkPropertyName.component(property).getString() + ": " + Math.round(chance * 100.0F) + "%"));
                }
            }
            if (lines.size() == 1) lines.add(Component.translatable("gui.mystcraft.ink_mixer.plain_ink"));
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }
}
