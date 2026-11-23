package com.dermegamecha.client.ui;

import com.dermegamecha.client.DerMegaMechaClient;
import com.dermegamecha.client.HudRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
    private final Screen parent;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Reset Preview"), button -> {
            DerMegaMechaClient.INSTANCE.config().pvpHud.cps = true;
            DerMegaMechaClient.INSTANCE.config().pvpHud.fps = true;
            DerMegaMechaClient.INSTANCE.config().pvpHud.coordinates = true;
        }).bounds(12, height - 32, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
            DerMegaMechaClient.INSTANCE.config().save();
            minecraft.setScreen(parent);
        }).bounds(width - 112, height - 32, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, 36, 0xFF10141B);
        graphics.drawString(font, "DerMegaMecha  HUD Editor", 12, 12, 0xFF4CC9F0);
        graphics.drawString(font, "Preview — drag support is available once an element position is saved", 12, 48, 0xFFB6C7D2);
        HudRenderer.render(graphics, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
