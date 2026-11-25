package com.dermegamecha.client.mixin;

import com.dermegamecha.client.HudRenderer;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void dermegamecha$registerClicks(long window, int button, int action, int modifiers, CallbackInfo callbackInfo) {
        if (action != GLFW.GLFW_PRESS) return;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) HudRenderer.registerLeftClick();
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) HudRenderer.registerRightClick();
    }
}
