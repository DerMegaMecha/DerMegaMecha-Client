package com.dermegamecha.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public General general = new General();
    public Performance performance = new Performance();
    public PvpHud pvpHud = new PvpHud();
    public BedWars bedWars = new BedWars();
    public Visuals visuals = new Visuals();
    public Crosshair crosshair = new Crosshair();
    public InterfaceSettings interfaceSettings = new InterfaceSettings();
    public Accessibility accessibility = new Accessibility();

    private transient long sessionStart = System.currentTimeMillis();
    private transient long lastFrameTime = System.nanoTime();
    private transient double smoothedFrameMs = 16.67;

    public void load() {
        Path file = getFile();
        try {
            if (Files.exists(file)) {
                ClientConfig loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), ClientConfig.class);
                if (loaded != null) copyFrom(loaded);
            } else save();
        } catch (Exception exception) {
            DerMegaMechaClient.LOGGER.error("Failed to load DerMegaMecha configuration", exception);
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(getFile().getParent());
            Files.writeString(getFile(), GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            DerMegaMechaClient.LOGGER.error("Failed to save DerMegaMecha configuration", exception);
        }
    }

    private Path getFile() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config")
                .resolve("dermegamecha-client.json");
    }

    private void copyFrom(ClientConfig source) {
        this.general = source.general != null ? source.general : new General();
        this.performance = source.performance != null ? source.performance : new Performance();
        this.pvpHud = source.pvpHud != null ? source.pvpHud : new PvpHud();
        this.bedWars = source.bedWars != null ? source.bedWars : new BedWars();
        this.visuals = source.visuals != null ? source.visuals : new Visuals();
        this.crosshair = source.crosshair != null ? source.crosshair : new Crosshair();
        this.interfaceSettings = source.interfaceSettings != null ? source.interfaceSettings : new InterfaceSettings();
        this.accessibility = source.accessibility != null ? source.accessibility : new Accessibility();
    }

    public void updateFrameAndSession() {
        long now = System.nanoTime();
        double frameMs = (now - lastFrameTime) / 1_000_000.0;
        lastFrameTime = now;
        smoothedFrameMs = smoothedFrameMs * 0.9 + frameMs * 0.1;
    }

    public double frameTimeMillis() { return smoothedFrameMs; }
    public long sessionMillis() { return System.currentTimeMillis() - sessionStart; }

    public static class General { public boolean enableHud = true; public boolean showServerCompatibilityNotice = true; public boolean showSessionTimer = true; }
    public static class Performance { public boolean fps = true; public boolean frameTime = true; public boolean memory = true; public boolean entityRenderOptimization = false; public boolean smoothFramePacing = false; }
    public static class PvpHud {
        public boolean cps = true; public boolean fps = true; public boolean ping = true; public boolean coordinates = true; public boolean direction = true;
        public boolean keystrokes = true; public boolean sprint = true; public boolean armor = true; public boolean heldItem = true; public boolean potions = true; public boolean clock = true;
    }
    public static class BedWars { public boolean enabled = false; public boolean star = false; public boolean stats = false; public boolean matchTimer = true; public boolean gameMode = true; public boolean teamInfo = false; public boolean bedStatus = false; }
    public static class Visuals { public boolean hitColor = true; public boolean damageIndicators = true; public boolean scoreboard = true; public boolean chat = true; public boolean itemTooltips = true; public boolean particleEffects = true; public boolean cleanInventory = true; public boolean playerNameTags = true; }
    public static class Crosshair { public boolean enabled = true; public int shape = 0; public int size = 8; public int thickness = 1; public int gap = 3; public boolean dynamic = false; public int color = 0xFFFFFF; }
    public static class InterfaceSettings { public int opacity = 220; public int backgroundColor = 0x171C24; public int accentColor = 0x4CC9F0; public boolean roundedPanels = true; }
    public static class Accessibility { public boolean highContrast = false; public float textScale = 1.0f; }
}
