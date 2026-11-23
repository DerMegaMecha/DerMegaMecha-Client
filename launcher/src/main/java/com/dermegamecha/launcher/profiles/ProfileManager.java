package com.dermegamecha.launcher.profiles;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ProfileManager {
    public record GameProfile(String id, String name, String minecraftVersion, String forgeVersion, Path gameDirectory,
                              int ramMb, int width, int height, boolean fullscreen, List<String> javaArguments) {}

    public List<GameProfile> list(Path config) throws Exception {
        Files.createDirectories(config);
        Path file = config.resolve("profiles.json");
        if (!Files.isRegularFile(file)) return List.of(defaultProfile(config));
        JsonArray array = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonArray();
        List<GameProfile> result = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject o = element.getAsJsonObject();
            List<String> args = new ArrayList<>();
            if (o.has("javaArguments")) for (JsonElement a : o.getAsJsonArray("javaArguments")) args.add(a.getAsString());
            result.add(new GameProfile(o.get("id").getAsString(), o.get("name").getAsString(),
                    o.get("minecraftVersion").getAsString(), o.get("forgeVersion").getAsString(),
                    Path.of(o.get("gameDirectory").getAsString()), o.get("ramMb").getAsInt(),
                    o.get("width").getAsInt(), o.get("height").getAsInt(), o.get("fullscreen").getAsBoolean(), args));
        }
        return result;
    }

    public GameProfile defaultProfile(Path config) { return new GameProfile("default", "DerMegaMecha PvP", "1.21.1", "52.0.28",
            config.resolve("..").normalize().resolve("instances").resolve("dermegmecha-1.21.1"), 4096, 1920, 1080, false, List.of()); }
}
