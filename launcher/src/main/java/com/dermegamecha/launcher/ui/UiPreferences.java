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
    public static String prismExecutable() { return STORE.get("prismExecutable", ""); }
    public static void setPrismExecutable(String value) { STORE.put("prismExecutable", value == null ? "" : value.trim()); }
    public static String packArchivePath() { return STORE.get("packArchivePath", ""); }
    public static void setPackArchivePath(String value) { STORE.put("packArchivePath", value == null ? "" : value.trim()); }
    public static String packArchiveSha512() { return STORE.get("packArchiveSha512", ""); }
    public static void setPackArchiveSha512(String value) { STORE.put("packArchiveSha512", value == null ? "" : value.trim()); }
    public static boolean prismImportConfirmed() { return STORE.getBoolean("prismImportConfirmed", false); }
    public static void setPrismImportConfirmed(boolean value) { STORE.putBoolean("prismImportConfirmed", value); }
    public static String prismInstance(String profile) { return STORE.get("prismInstance." + profile, ""); }
    public static void setPrismInstance(String profile, String value) {
        STORE.put("prismInstance." + profile, value == null ? "" : value.trim());
    }
    public static String prismProfile() { return STORE.get("prismProfile", "forge"); }
    public static void setPrismProfile(String value) {
        STORE.put("prismProfile", "fabric".equalsIgnoreCase(value) ? "fabric" : "forge");
    }
    public static String serverName() { return STORE.get("serverName", "DerMegaMecha"); }
    public static void setServerName(String value) { STORE.put("serverName", value == null ? "" : value.trim()); }
    public static String serverAddress() { return STORE.get("serverAddress", ""); }
    public static void setServerAddress(String value) { STORE.put("serverAddress", value == null ? "" : value.trim()); }
    public static String serverMinecraftVersion() { return STORE.get("serverMinecraftVersion", "1.21.1"); }
    public static void setServerMinecraftVersion(String value) {
        STORE.put("serverMinecraftVersion", value == null ? "" : value.trim());
    }
    public static String serverPackVersion() { return STORE.get("serverPackVersion", ""); }
    public static void setServerPackVersion(String value) { STORE.put("serverPackVersion", value == null ? "" : value.trim()); }
    public static String text(String de, String en) { return english() ? en : de; }
    public static Locale locale() { return english() ? Locale.ENGLISH : Locale.GERMAN; }
}
