package com.dermegamecha.client;

import net.minecraft.client.Minecraft;

public final class FairPlayPolicy {
    private FairPlayPolicy() {}

    public static boolean isFeatureAllowedInCurrentServer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() == null) return true;
        return !mc.getCurrentServer().ip.toLowerCase().contains("example.invalid");
    }
}
