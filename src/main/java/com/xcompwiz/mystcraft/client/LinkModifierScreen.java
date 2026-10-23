package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.inventory.LinkModifierMenu;
import com.xcompwiz.mystcraft.linking.LinkProperties;
import com.xcompwiz.mystcraft.network.LinkModifierTextPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** Legacy 176x166 Link Modifier editor: flags, Age seed, title and dead-Age control. */
public final class LinkModifierScreen extends AbstractContainerScreen<LinkModifierMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "gui/single_slot.png");
    private EditBox seedBox;
    private EditBox titleBox;
    private boolean syncing;
    private boolean deadArmed;
    private String lastBookSignature = "";

    public LinkModifierScreen(LinkModifierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        inventoryLabelX = 8;
        inventoryLabelY = 72;
        titleLabelY = -1000; // legacy GUI had no conventional container title label
    }

    @Override protected void init() {
        super.init();
        seedBox = new EditBox(font, leftPos + 80, topPos + 15, 87, 14, Component.translatable("gui.mystcraft.link_modifier.seed"));
        seedBox.setMaxLength(21);
        seedBox.setBordered(true);
        seedBox.setResponder(value -> {
            if (!syncing) net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new LinkModifierTextPayload(menu.containerId, LinkModifierTextPayload.FIELD_SEED, value));
        });
        addRenderableWidget(seedBox);

        titleBox = new EditBox(font, leftPos + 80, topPos + 56, 87, 14, Component.translatable("gui.mystcraft.book_title"));
        titleBox.setMaxLength(21);
        titleBox.setBordered(true);
        titleBox.setResponder(value -> {
            if (!syncing) net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new LinkModifierTextPayload(menu.containerId, LinkModifierTextPayload.FIELD_TITLE, value));
        });
        addRenderableWidget(titleBox);
        syncFields(true);
    }

    @Override protected void containerTick() {
        super.containerTick();
        syncFields(false);
        boolean seedVisible = menu.hasSeed();
        seedBox.visible = seedVisible;
        seedBox.setEditable(seedVisible);
        if (!seedVisible) deadArmed = false;
    }

    private void syncFields(boolean force) {
        if (seedBox == null || titleBox == null) return;
        String signature = menu.getSlot(0).getItem().isEmpty() ? "" : menu.getSlot(0).getItem().getHoverName().getString() + menu.getDimensionText();
        if (!force && signature.equals(lastBookSignature)) return;
        lastBookSignature = signature;
        syncing = true;
        seedBox.setValue(menu.getSeedText());
        titleBox.setValue(menu.getBookTitle());
        syncing = false;
        deadArmed = false;
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        LegacyGuiDrawing.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        for (int i=0; i<menu.properties().size(); i++) {
            int col = i / 3;
            int row = i % 3;
            int x = leftPos + 5 + col*20;
            int y = topPos + 10 + row*20;
            drawLegacyToggleSquare(graphics, x, y, menu.propertyEnabled(i), false);
        }

        if (menu.hasSeed()) {
            int x = leftPos + (deadArmed || menu.isDead() ? 140 : 120);
            int y = topPos + 32;
            drawLegacyToggleSquare(graphics, x, y, menu.isDead(), deadArmed || menu.isDead());
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.translatable("container.inventory"), inventoryLabelX, inventoryLabelY, 0xFF404040, false);
        String dim = menu.getDimensionText();
        if (!dim.isBlank()) graphics.text(font, dim, 100, 40, 0xFFFFFFFF, true);
    }

    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        for (int i=0; i<menu.properties().size(); i++) {
            int col=i/3, row=i%3;
            int x=leftPos+5+col*20, y=topPos+10+row*20;
            if (mouseX>=x && mouseX<x+18 && mouseY>=y && mouseY<y+18) {
                clickMenuButton(LinkModifierMenu.ACTION_TOGGLE_BASE+i);
                return true;
            }
        }
        if (menu.hasSeed() && !menu.isDead()) {
            int x=leftPos+(deadArmed?140:120), y=topPos+32;
            if (mouseX>=x && mouseX<x+18 && mouseY>=y && mouseY<y+18) {
                if (deadArmed) clickMenuButton(LinkModifierMenu.ACTION_MARK_DEAD);
                else deadArmed=true;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void clickMenuButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractTooltip(graphics, mouseX, mouseY);
        for (int i=0; i<menu.properties().size(); i++) {
            int col=i/3,row=i%3,x=leftPos+5+col*20,y=topPos+10+row*20;
            if (mouseX>=x && mouseX<x+18 && mouseY>=y && mouseY<y+18) {
                graphics.setTooltipForNextFrame(font, propertyName(menu.properties().get(i)), mouseX, mouseY);
                return;
            }
        }
        if (menu.hasSeed()) {
            int x=leftPos+(deadArmed||menu.isDead()?140:120),y=topPos+32;
            if (mouseX>=x && mouseX<x+18 && mouseY>=y && mouseY<y+18) {
                graphics.setTooltipForNextFrame(font, Component.translatable(menu.isDead()
                        ? "gui.mystcraft.link_modifier.dead_already"
                        : deadArmed ? "gui.mystcraft.link_modifier.dead_confirm" : "gui.mystcraft.link_modifier.dead_arm"), mouseX, mouseY);
            }
        }
    }

    /** Matches the plain square GuiElementButtonToggle used by 0.13.7.06. */
    private static void drawLegacyToggleSquare(GuiGraphicsExtractor graphics, int x, int y, boolean depressed, boolean redTint) {
        int inner = redTint ? 0xFF8B4040 : 0xFF8B8B8B;
        int light = redTint ? 0xFFFF8080 : 0xFFFFFFFF;
        int dark = 0xFF373737;
        int topLeft = depressed ? dark : light;
        int bottomRight = depressed ? light : dark;
        graphics.fill(x, y, x + 18, y + 18, inner);
        graphics.fill(x, y, x + 18, y + 1, topLeft);
        graphics.fill(x, y, x + 1, y + 18, topLeft);
        graphics.fill(x, y + 17, x + 18, y + 18, bottomRight);
        graphics.fill(x + 17, y, x + 18, y + 18, bottomRight);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, inner);
    }

    private static Component propertyName(String property) {
        return switch (property) {
            case LinkProperties.INTRA_LINKING -> Component.translatable("linkeffect.intralinking.name");
            case LinkProperties.INTRA_LINKING_ONLY -> Component.translatable("linkeffect.intralinkingonly.name");
            case LinkProperties.GENERATE_PLATFORM -> Component.translatable("linkeffect.generateplatform.name");
            case LinkProperties.MAINTAIN_MOMENTUM -> Component.translatable("linkeffect.maintainmomentum.name");
            case LinkProperties.DISARM -> Component.translatable("linkeffect.disarm.name");
            case LinkProperties.RELATIVE -> Component.translatable("linkeffect.relative.name");
            default -> Component.literal(property);
        };
    }
}
