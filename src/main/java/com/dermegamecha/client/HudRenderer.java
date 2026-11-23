package com.dermegamecha.client;

import com.dermegamecha.client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class HudRenderer {
    private HudRenderer() {}

    private static long leftClicks;
    private static long rightClicks;

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        ClientConfig config = com.dermegamecha.client.DerMegaMechaClient.INSTANCE.config();
        int x = 8;
        int y = 8;
        int accent = config.interfaceSettings.accentColor;
        int background = (config.interfaceSettings.opacity << 24) | (config.interfaceSettings.backgroundColor & 0xFFFFFF);

        graphics.drawString(mc.font, "§bDerMegaMecha §7Client", x + 6, y + 5, 0xFFFFFF);
        int height = 24;

        if (config.pvpHud.fps || config.performance.fps) {
            drawStat(graphics, mc, "FPS", Integer.toString(mc.getFps()), x, y + height, accent, background);
            height += 14;
        }
        if (config.performance.frameTime) {
            drawStat(graphics, mc, "Frame", String.format("%.1f ms", config.frameTimeMillis()), x, y + height, accent, background);
            height += 14;
        }
        if (config.performance.memory) {
            Runtime runtime = Runtime.getRuntime();
            long used = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
            long max = runtime.maxMemory() / 1048576L;
            drawStat(graphics, mc, "Memory", used + " / " + max + " MB", x, y + height, accent, background);
            height += 14;
        }
        if (config.pvpHud.ping && mc.getCurrentServer() != null) {
            drawStat(graphics, mc, "Ping", mc.getSingleplayerServer() == null ? mc.getConnection() != null && mc.player != null ? "Live" : "-" : "0 ms", x, y + height, accent, background);
            height += 14;
        }
        if (config.pvpHud.coordinates) {
            drawStat(graphics, mc, "XYZ", String.format("%.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()), x, y + height, accent, background);
            height += 14;
        }
        if (config.pvpHud.direction) {
            drawStat(graphics, mc, "Facing", facing(mc.player.getYRot()), x, y + height, accent, background);
            height += 14;
        }
        if (config.general.showSessionTimer) {
            drawStat(graphics, mc, "Session", formatDuration(config.sessionMillis()), x, y + height, accent, background);
        }

        if (config.pvpHud.keystrokes) renderKeystrokes(graphics, mc, x, mc.getWindow().getGuiScaledHeight() - 58, accent, background);

        if (config.pvpHud.cps) {
            graphics.fill(mc.getWindow().getGuiScaledWidth() - 86, 8,
                    mc.getWindow().getGuiScaledWidth() - 8, 40, background);
            graphics.drawString(mc.font, "L: " + leftClicks + "  R: " + rightClicks,
                    mc.getWindow().getGuiScaledWidth() - 78, 18, 0xEAFBFF);
        }

        if (config.pvpHud.potions) renderPotionStatus(graphics, mc);
        if (config.crosshair.enabled) renderCrosshair(graphics, mc, config);
    }

    private static void drawStat(GuiGraphics g, Minecraft mc, String label, String value, int x, int y, int accent, int background) {
        g.fill(x, y, x + 145, y + 12, background);
        g.fill(x, y, x + 2, y + 12, accent);
        g.drawString(mc.font, label + "  §f" + value, x + 6, y + 3, 0xDCE8F0);
    }

    private static void renderKeystrokes(GuiGraphics g, Minecraft mc, int x, int y, int accent, int bg) {
        int size = 16;
        g.fill(x, y + 18, x + size, y + 34, mc.options.keyUp.isDown() ? accent : bg);
        g.drawCenteredString(mc.font, "W", x + size / 2, y + 23, 0xFFFFFF);
        g.fill(x - 18, y + 36, x - 2, y + 52, mc.options.keyLeft.isDown() ? accent : bg);
        g.fill(x, y + 36, x + size, y + 52, mc.options.keyDown.isDown() ? accent : bg);
        g.fill(x + 18, y + 36, x + 34, y + 52, mc.options.keyRight.isDown() ? accent : bg);
        g.drawCenteredString(mc.font, "A", x - 10, y + 41, 0xFFFFFF);
        g.drawCenteredString(mc.font, "S", x + 8, y + 41, 0xFFFFFF);
        g.drawCenteredString(mc.font, "D", x + 26, y + 41, 0xFFFFFF);
    }

    private static void renderPotionStatus(GuiGraphics g, Minecraft mc) {
        int y = 8;
        int width = mc.getWindow().getGuiScaledWidth();
        for (var effect : mc.player.getActiveEffects()) {
            String text = Component.translatable(effect.getDescriptionId()).getString() + " " + (effect.getAmplifier() + 1);
            g.fill(width - 145, y, width - 8, y + 12, 0xCC171C24);
            g.drawString(mc.font, text, width - 139, y + 3, 0xEAFBFF);
            y += 14;
        }
    }

    private static void renderCrosshair(GuiGraphics g, Minecraft mc, ClientConfig c) {
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;
        ClientConfig.Crosshair ch = c.crosshair;
        int color = 0xFF000000 | ch.color;
        int gap = ch.gap;
        g.fill(cx - ch.thickness, cy - gap - ch.size, cx + ch.thickness, cy - gap, color);
        g.fill(cx - ch.thickness, cy + gap, cx + ch.thickness, cy + gap + ch.size, color);
        g.fill(cx - gap - ch.size, cy - ch.thickness, cx - gap, cy + ch.thickness, color);
        g.fill(cx + gap, cy - ch.thickness, cx + gap + ch.size, cy + ch.thickness, color);
    }

    private static String facing(float yaw) {
        float normalized = ((yaw % 360) + 360) % 360;
        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        return dirs[Math.round(normalized / 45.0f) & 7];
    }

    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    public static void registerLeftClick() { leftClicks = (leftClicks + 1) % 1000; }
    public static void registerRightClick() { rightClicks = (rightClicks + 1) % 1000; }
}
