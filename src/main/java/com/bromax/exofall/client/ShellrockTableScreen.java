package com.bromax.exofall.client;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.crafting.ShellrockTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ShellrockTableScreen extends AbstractContainerScreen<ShellrockTableMenu> {
    private static final ResourceLocation BG = Exofall.id("textures/gui/container/shellrock_crafting_table.png");

    public ShellrockTableScreen(ShellrockTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 206;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelY = ShellrockTableMenu.INV_Y - 11;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
