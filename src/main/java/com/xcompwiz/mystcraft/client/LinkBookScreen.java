package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.api.impl.InternalAPI;
import com.xcompwiz.mystcraft.api.impl.LinkInfoAdapter;
import com.xcompwiz.mystcraft.api.impl.runtime.ApiRenderContext;
import com.xcompwiz.mystcraft.api.impl.runtime.ApiSymbolView;
import com.xcompwiz.mystcraft.api.util.Color;
import com.xcompwiz.mystcraft.inventory.LinkBookMenu;
import com.xcompwiz.mystcraft.network.LinkBookActivatePayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * 1.21.1 reconstruction of the 0.13.7.06 two-page Mystcraft book UI.
 * The original cover/page textures and 327x199 interaction geometry are retained.
 */
public final class LinkBookScreen extends AbstractContainerScreen<LinkBookMenu> {
    private static final Identifier COVER = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "gui/bookui_cover.png");
    private static final Identifier PAGE_LEFT = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "gui/bookui_pagel.png");
    private static final Identifier PAGE_RIGHT = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "gui/bookui_pager.png");
    private static final Identifier PAGE_RIGHT_FULL = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "gui/bookui_rpage_full.png");
    private int currentPage;

    public LinkBookScreen(LinkBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 327, 199);
        titleLabelX = -1000;
        inventoryLabelX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        currentPage = 0;
        for (var effect : InternalAPI.renderEffects()) {
            try { effect.onOpen(); }
            catch (RuntimeException failure) { Mystcraft.LOGGER.error("Mystcraft Link Panel effect failed during onOpen", failure); }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Exact legacy cover composition: borders, left board, spine and right board.
        LegacyGuiDrawing.blit(graphics, COVER, leftPos, topPos + 7, 152, 0, 34, 192, 256, 256);
        LegacyGuiDrawing.blit(graphics, COVER, leftPos + 34, topPos + 7, 49, 0, 103, 192, 256, 256);
        LegacyGuiDrawing.blit(graphics, COVER, leftPos + 137, topPos + 7, 45, 0, 4, 192, 256, 256);
        LegacyGuiDrawing.blit(graphics, COVER, leftPos + 141, topPos + 7, 0, 0, 186, 192, 256, 256);
        if (menu.isAgebook()) {
            // 0.13.7.06 drew the gold Descriptive Book border from the cover sheet.
            LegacyGuiDrawing.blit(graphics, COVER, leftPos, topPos + 7, 186, 0, 34, 192, 256, 256);
            LegacyGuiDrawing.blit(graphics, COVER, leftPos + 293, topPos + 7, 186, 0, 34, 192, 256, 256);
        }

        if (currentPage > 0) {
            LegacyGuiDrawing.blit(graphics, PAGE_LEFT, leftPos + 7, topPos, 0, 0, 156, 195, 256, 256);
        }

        if (currentPage < menu.getPageCount()) {
            String pageId = menu.getPageIds().get(currentPage);
            if ("$LINK_PANEL$".equals(pageId)) {
                drawLinkPanel(graphics);
                LegacyGuiDrawing.blit(graphics, PAGE_RIGHT, leftPos + 163, topPos, 0, 0, 156, 195, 256, 256);
            } else {
                LegacyGuiDrawing.blit(graphics, PAGE_RIGHT_FULL, leftPos + 163, topPos, 0, 0, 156, 195, 256, 256);
                drawSymbolPage(graphics, pageId);
            }
        }
    }

    private void drawLinkPanel(GuiGraphicsExtractor graphics) {
        int x1 = leftPos + 173;
        int y1 = topPos + 20;
        int x2 = x1 + 132;
        int y2 = y1 + 83;
        boolean established = menu.getDimensionKey() != null && !menu.getDimensionKey().isBlank();
        if (established) graphics.fillGradient(x1, y1, x2, y2, 0xFF000044, 0xFF006666);
        else graphics.fill(x1, y1, x2, y2, 0xFF000000);

        renderApiEffects(graphics, x1, y1, x2 - x1, y2 - y1);

        String destination = menu.getDestinationName();
        if (destination != null && !destination.isBlank()) {
            graphics.centeredText(font, Component.literal(destination), leftPos + 239, topPos + 111, 0xFF303030);
        }
    }

    private void renderApiEffects(GuiGraphicsExtractor graphics, int left, int top, int width, int height) {
        if (minecraft == null || minecraft.player == null || InternalAPI.renderEffects().isEmpty()) return;
        ItemStack held;
        if (menu.isDisplaySource() && menu.getDisplayPos() != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.getDisplayPos()) instanceof com.xcompwiz.mystcraft.blockentity.BookDisplayBlockEntity display) {
            held = display.getBook();
        } else {
            held = minecraft.player.getItemInHand(menu.getHand());
        }
        if (held.isEmpty()) return;
        ItemStack clone = held.copy();
        var linkInfo = LinkInfoAdapter.forStack(clone);
        ApiRenderContext.push(new ApiRenderContext.Renderer() {
            @Override public void drawWord(float x, float y, float z, float scale, String word) {
                LegacySymbolRenderer.drawWord(graphics, word, Math.round(x), Math.round(y), Math.max(1, Math.round(scale)));
            }
            @Override public void drawSymbol(float x, float y, float z, float scale, Identifier identifier) {
                String legacy = ApiSymbolView.legacyIdFor(identifier);
                LegacySymbolRenderer.drawSymbol(graphics, legacy == null ? identifier.toString() : legacy, Math.round(x), Math.round(y), Math.max(1, Math.round(scale)));
            }
            @Override public void drawColorEye(float x, float y, float z, float radius, Color color) {
                int rgb = color == null ? 0xFFFFFFFF : color.asInt();
                int argb = 0xFF000000 | rgb;
                int cx = Math.round(x), cy = Math.round(y), r = Math.max(1, Math.round(radius));
                for (int dy = -r; dy <= r; ++dy) {
                    int dx = (int)Math.floor(Math.sqrt(Math.max(0, r * r - dy * dy)));
                    graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, argb);
                }
                int pupil = Math.max(1, r / 3);
                graphics.fill(cx - pupil, cy - pupil, cx + pupil + 1, cy + pupil + 1, 0xFF000000);
            }
        });
        try {
            for (var effect : InternalAPI.renderEffects()) {
                try { effect.render(left, top, width, height, linkInfo.clone(), clone.copy()); }
                catch (RuntimeException failure) { Mystcraft.LOGGER.error("Mystcraft Link Panel render effect failed", failure); }
            }
        } finally {
            ApiRenderContext.pop();
        }
    }

    private void drawSymbolPage(GuiGraphicsExtractor graphics, String pageId) {
        if ("$BLANK$".equals(pageId) || "$PAGE$".equals(pageId)) return;
        // Exact legacy GuiElementBook placement: x=171, y=25, scale=140.
        LegacySymbolRenderer.drawSymbol(graphics, pageId, leftPos + 171, topPos + 25, 140);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (currentPage == 0) {
            graphics.text(font, Component.literal(menu.getDestinationName()), 40, 40, 0xFF000000, false);
            int y = 50;
            for (String author : menu.getAuthors()) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(50, y);
                graphics.pose().scale(0.5F, 0.5F);
                graphics.text(font, Component.literal(author), 0, 0, 0xFF000000, false);
                graphics.pose().popMatrix();
                y += 5;
            }
        }
        String pageText = currentPage + "/" + menu.getPageCount();
        graphics.centeredText(font, Component.literal(pageText), 165, 185, 0xFF000000);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            double x = mouseX - leftPos;
            double y = mouseY - topPos;
            if (currentPage < menu.getPageCount() && "$LINK_PANEL$".equals(menu.getPageIds().get(currentPage))
                    && x >= 173 && x <= 305 && y >= 20 && y <= 103) {
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new LinkBookActivatePayload(menu.containerId));
                return true;
            }
            if (x >= 0 && x <= 156 && y >= 0 && y <= 195) {
                currentPage = Math.max(0, currentPage - 1);
                return true;
            }
            if (x >= 158 && x <= 312 && y >= 0 && y <= 195) {
                currentPage = Math.min(menu.getPageCount(), currentPage + 1);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == InputConstants.KEY_LEFT) {
            currentPage = Math.max(0, currentPage - 1);
            return true;
        }
        if (keyCode == InputConstants.KEY_RIGHT) {
            currentPage = Math.min(menu.getPageCount(), currentPage + 1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractTooltip(graphics, mouseX, mouseY);
        if (currentPage < menu.getPageCount()) {
            String pageId = menu.getPageIds().get(currentPage);
            if (!"$LINK_PANEL$".equals(pageId) && !"$BLANK$".equals(pageId) && !"$PAGE$".equals(pageId)
                    && mouseX >= leftPos + 171 && mouseX < leftPos + 311
                    && mouseY >= topPos + 25 && mouseY < topPos + 165) {
                graphics.setTooltipForNextFrame(font, LegacySymbolName.component(pageId), mouseX, mouseY);
            }
        }
    }
}
