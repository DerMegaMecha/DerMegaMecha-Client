package com.dermegamecha.client.ui;

import com.dermegamecha.client.DerMegaMechaClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ClientSettingsScreen extends Screen {
    private final Screen parent;
    private Category category = Category.GENERAL;
    private final List<CategoryTab> tabs = new ArrayList<>();

    public ClientSettingsScreen(Screen parent) {
        super(Component.literal("DerMegaMecha Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        tabs.clear();
        int left = 12;
        int y = 52;
        for (Category value : Category.values()) {
            final Category selected = value;
            tabs.add(new CategoryTab(left, y, 112, 20, Component.literal(value.title), button -> category = selected));
            addRenderableWidget(tabs.get(tabs.size() - 1));
            y += 24;
        }

        int panelX = 136;
        int panelWidth = width - panelX - 12;
        int optionY = 52;
        if (category == Category.GENERAL) {
            addToggle(panelX, optionY, panelWidth, "Enable DerMegaMecha HUD", DerMegaMechaClient.INSTANCE.config().general.enableHud,
                    value -> DerMegaMechaClient.INSTANCE.config().general.enableHud = value); optionY += 28;
            addToggle(panelX, optionY, panelWidth, "Session timer", DerMegaMechaClient.INSTANCE.config().general.showSessionTimer,
                    value -> DerMegaMechaClient.INSTANCE.config().general.showSessionTimer = value); optionY += 28;
        } else if (category == Category.PERFORMANCE) {
            addToggle(panelX, optionY, panelWidth, "FPS display", DerMegaMechaClient.INSTANCE.config().performance.fps,
                    value -> DerMegaMechaClient.INSTANCE.config().performance.fps = value); optionY += 28;
            addToggle(panelX, optionY, panelWidth, "Frame-time display", DerMegaMechaClient.INSTANCE.config().performance.frameTime,
                    value -> DerMegaMechaClient.INSTANCE.config().performance.frameTime = value); optionY += 28;
            addToggle(panelX, optionY, panelWidth, "Memory usage", DerMegaMechaClient.INSTANCE.config().performance.memory,
                    value -> DerMegaMechaClient.INSTANCE.config().performance.memory = value); optionY += 28;
            addToggle(panelX, optionY, panelWidth, "Entity rendering optimization", DerMegaMechaClient.INSTANCE.config().performance.entityRenderOptimization,
                    value -> DerMegaMechaClient.INSTANCE.config().performance.entityRenderOptimization = value);
        } else if (category == Category.PVP_HUD) {
            addToggle(panelX, optionY, panelWidth, "CPS counter", DerMegaMechaClient.INSTANCE.config().pvpHud.cps,
                    value -> DerMegaMechaClient.INSTANCE.config().pvpHud.cps = value); optionY += 28;
            addToggle(panelX, optionY, panelWidth, "Coordinates", DerMegaMechaClient.INSTANCE.config().pvpHud.coordinates,
                    value -> DerMegaMechaClient.INSTANCE.config().pvpHud.coordinates = value); optionY += 28;
            addToggle(panelX, optionY, panelWidth, "Keystrokes", DerMegaMechaClient.INSTANCE.config().pvpHud.keystrokes,
                    value -> DerMegaMechaClient.INSTANCE.config().pvpHud.keystrokes = value); optionY += 28;
            addToggle(panelX, optionY, panelWidth, "Potion indicators", DerMegaMechaClient.INSTANCE.config().pvpHud.potions,
                    value -> DerMegaMechaClient.INSTANCE.config().pvpHud.potions = value);
        } else if (category == Category.BEDWARS) {
            addToggle(panelX, optionY, panelWidth, "Enable supported BedWars HUD", DerMegaMechaClient.INSTANCE.config().bedWars.enabled,
                    value -> DerMegaMechaClient.INSTANCE.config().bedWars.enabled = value); optionY += 28;
            addToggle(panelX, optionY, panelWidth, "Public BedWars star", DerMegaMechaClient.INSTANCE.config().bedWars.star,
                    value -> DerMegaMechaClient.INSTANCE.config().bedWars.star = value); optionY += 28;
            addToggle(panelX, optionY, panelWidth, "Match timer", DerMegaMechaClient.INSTANCE.config().bedWars.matchTimer,
                    value -> DerMegaMechaClient.INSTANCE.config().bedWars.matchTimer = value);
        } else if (category == Category.CROSSHAIR) {
            addToggle(panelX, optionY, panelWidth, "Custom crosshair", DerMegaMechaClient.INSTANCE.config().crosshair.enabled,
                    value -> DerMegaMechaClient.INSTANCE.config().crosshair.enabled = value); optionY += 28;
            addToggle(panelX, optionY, panelWidth, "Dynamic crosshair", DerMegaMechaClient.INSTANCE.config().crosshair.dynamic,
                    value -> DerMegaMechaClient.INSTANCE.config().crosshair.dynamic = value); optionY += 28;
            addButton(panelX, optionY, panelWidth, "Open HUD Editor", button -> minecraft.setScreen(new HudEditorScreen(this)));
        } else {
            addToggle(panelX, optionY, panelWidth, "High contrast", DerMegaMechaClient.INSTANCE.config().accessibility.highContrast,
                    value -> DerMegaMechaClient.INSTANCE.config().accessibility.highContrast = value); optionY += 28;
            addToggle(panelX, optionY, panelWidth, "Server compatibility notice", DerMegaMechaClient.INSTANCE.config().general.showServerCompatibilityNotice,
                    value -> DerMegaMechaClient.INSTANCE.config().general.showServerCompatibilityNotice = value);
        }

        addRenderableWidget(Button.builder(Component.literal("Save & Close"), button -> {
            DerMegaMechaClient.INSTANCE.config().save();
            minecraft.setScreen(parent);
        }).bounds(width - 112, height - 28, 100, 20).build());
    }

    private void addToggle(int x, int y, int width, String label, boolean initial, java.util.function.Consumer<Boolean> setter) {
        final boolean[] state = {initial};
        addRenderableWidget(Button.builder(toggleLabel(label, state[0]), button -> {
            state[0] = !state[0];
            setter.accept(state[0]);
            button.setMessage(toggleLabel(label, state[0]));
            DerMegaMechaClient.INSTANCE.config().save();
        }).bounds(x, y, width, 20).build());
    }

    private void addButton(int x, int y, int width, String label, Button.OnPress press) {
        addRenderableWidget(Button.builder(Component.literal(label), press).bounds(x, y, width, 20).build());
    }

    private static Component toggleLabel(String label, boolean state) {
        return Component.literal(label + "  [" + (state ? "ON" : "OFF") + "]");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int bg = 0xFF171C24;
        graphics.fill(0, 0, width, 44, 0xFF10141B);
        graphics.fill(12, 48, 124, height - 12, 0xCC202631);
        graphics.fill(136, 48, width - 12, height - 12, 0xCC1D232C);
        graphics.drawString(font, "DERMEGAMECHA", 16, 12, 0xFF4CC9F0);
        graphics.drawString(font, "CLIENT", 16 + font.width("DERMEGAMECHA") + 5, 12, 0xFFFFFFFF);
        graphics.drawString(font, category.title, 148, 57, 0xFFEAFBFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        DerMegaMechaClient.INSTANCE.config().save();
        minecraft.setScreen(parent);
    }

    private enum Category {
        GENERAL("General"), PERFORMANCE("Performance"), PVP_HUD("PvP HUD"), BEDWARS("BedWars"),
        VISUALS("Visuals"), CROSSHAIR("Crosshair"), INTERFACE("Interface"), ACCESSIBILITY("Accessibility");
        final String title;
        Category(String title) { this.title = title; }
    }

    private static class CategoryTab extends Button {
        CategoryTab(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }
    }
}
