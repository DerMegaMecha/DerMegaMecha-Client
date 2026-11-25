package com.dermegamecha.client;

import com.dermegamecha.client.config.ClientConfig;
import com.dermegamecha.client.ui.ClientSettingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Mod(DerMegaMechaClient.MOD_ID)
public final class DerMegaMechaClient {
    public static final String MOD_ID = "dermegamecha";
    public static final String NAME = "DerMegaMecha Client";
    public static final String VERSION = "1.0.0";
    public static final String TAGLINE = "Built for Performance. Designed for PvP. Fair by Nature.";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public static final DerMegaMechaClient INSTANCE = new DerMegaMechaClient();
    private final ClientConfig config = new ClientConfig();

    public DerMegaMechaClient() {
        config.load();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientEvents::onKeyRegistration);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        Runtime.getRuntime().addShutdownHook(new Thread(config::save));
    }

    public ClientConfig config() { return config; }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        config.updateFrameAndSession();
        if (ClientEvents.OPEN_SETTINGS != null && ClientEvents.OPEN_SETTINGS.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            mc.setScreen(new ClientSettingsScreen(mc.screen));
        }
    }

    @SubscribeEvent
    public void onMouseButton(InputEvent.MouseButton.Post event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) HudRenderer.registerLeftClick();
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) HudRenderer.registerRightClick();
    }

    @SubscribeEvent
    public void onChatOverlay(CustomizeGuiOverlayEvent.Chat event) {
        if (config.general.enableHud) HudRenderer.render(event.getGuiGraphics());
    }

    public static final class ClientEvents {
        public static KeyMapping OPEN_SETTINGS;

        public static void onKeyRegistration(RegisterKeyMappingsEvent event) {
            OPEN_SETTINGS = new KeyMapping("key.dermegamecha.settings", GLFW.GLFW_KEY_RIGHT_SHIFT,
                    "key.categories.misc");
            event.register(OPEN_SETTINGS);
        }
    }
}
