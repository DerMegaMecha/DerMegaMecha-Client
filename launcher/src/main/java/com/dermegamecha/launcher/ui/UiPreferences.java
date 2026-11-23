package com.dermegamecha.launcher.ui;

import java.util.Locale;
import java.util.prefs.Preferences;

/** Small user-facing language/accessibility preference store shared by launcher screens. */
public final class UiPreferences {
    private static final Preferences STORE = Preferences.userRoot().node("DerMegaMecha/Launcher");
    private UiPreferences() {}

    public static boolean english() { return STORE.get("language", "de").equals("en"); }
    public static void setEnglish(boolean value) { STORE.put("language", value ? "en" : "de"); }
    public static boolean highContrast() { return STORE.getBoolean("highContrast", false); }
    public static void setHighContrast(boolean value) { STORE.putBoolean("highContrast", value); }
    public static double textScale() { return STORE.getDouble("textScale", 1.0); }
    public static void setTextScale(double value) { STORE.putDouble("textScale", Math.max(0.9, Math.min(1.35, value))); }
    public static String text(String de, String en) { return english() ? en : de; }
    public static Locale locale() { return english() ? Locale.ENGLISH : Locale.GERMAN; }
}
