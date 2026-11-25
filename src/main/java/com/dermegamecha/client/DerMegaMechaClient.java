package com.dermegamecha.client;

import com.dermegamecha.client.config.ClientConfig;
import com.dermegamecha.client.ui.ClientSettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public final class DerMegaMechaClient implements ClientModInitializer {
    public static final String MOD_ID = "dermegamecha";
    public static final String NAME = "DerMegaMecha Client";
    public static final String VERSION = "1.0.0";
    public static final String TAGLINE = "Built for Performance. Designed for PvP. Fair by Nature.";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public static DerMegaMechaClient INSTANCE;
    private final ClientConfig config = new ClientConfig();
    private KeyMapping openSettings;

    public DerMegaMechaClient() {
        INSTANCE = this;
    }

    @Override
    public void onInitializeClient() {
        config.load();
        Runtime.getRuntime().addShutdownHook(new Thread(config::save));

        openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.dermegamecha.settings",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.CATEGORY_MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            config.updateFrameAndSession();
            if (openSettings.consumeClick()) {
                client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                client.setScreen(new ClientSettingsScreen(client.screen));
            }
        });

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            if (config.general.enableHud) HudRenderer.render(graphics);
        });
    }

    public ClientConfig config() { return config; }
}
