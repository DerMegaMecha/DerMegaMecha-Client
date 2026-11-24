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
        if (!Files.isRegularFile(file)) {
            List<GameProfile> defaults = defaultProfiles(config);
            save(config, defaults);
            return defaults;
        }
        JsonArray array = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonArray();
        List<GameProfile> result = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject o = element.getAsJsonObject();
            List<String> args = new ArrayList<>();
            if (o.has("javaArguments")) for (JsonElement a : o.getAsJsonArray("javaArguments")) args.add(a.getAsString());
            result.add(new GameProfile(o.get("id").getAsString(), o.get("name").getAsString(),
                    o.get("minecraftVersion").getAsString(),
                    o.has("loaderVersion") ? o.get("loaderVersion").getAsString()
                        : (o.has("forgeVersion") ? o.get("forgeVersion").getAsString() : ""),
                    Path.of(o.get("gameDirectory").getAsString()), o.get("ramMb").getAsInt(),
                    o.get("width").getAsInt(), o.get("height").getAsInt(), o.get("fullscreen").getAsBoolean(), args));
        }
        return result;
    }

    public List<GameProfile> defaultProfiles(Path config) {
        Path instances = config.resolve("..").normalize().resolve("instances");
        return List.of(
            new GameProfile("forge", "DerMegaMecha - Forge 1.21.1", "1.21.1", "52.0.28",
                instances.resolve("dermegmecha-forge-1.21.1"), 4096, 1920, 1080, false, List.of()),
            new GameProfile("fabric", "DerMegaMecha - Fabric Modpack 1.21.1", "1.21.1", "fabric",
                instances.resolve("dermegmecha-fabric-1.21.1"), 4096, 1920, 1080, false, List.of())
        );
    }

    public GameProfile defaultProfile(Path config) { return defaultProfiles(config).get(0); }

    public void save(Path config, List<GameProfile> profiles) throws Exception {
        Files.createDirectories(config);
        JsonArray array = new JsonArray();
        for (GameProfile profile : profiles) {
            JsonObject object = new JsonObject();
            object.addProperty("id", profile.id());
            object.addProperty("name", profile.name());
            object.addProperty("minecraftVersion", profile.minecraftVersion());
            object.addProperty("loaderVersion", profile.forgeVersion());
            object.addProperty("gameDirectory", profile.gameDirectory().toString());
            object.addProperty("ramMb", profile.ramMb());
            object.addProperty("width", profile.width());
            object.addProperty("height", profile.height());
            object.addProperty("fullscreen", profile.fullscreen());
            JsonArray args = new JsonArray();
            for (String argument : profile.javaArguments()) args.add(argument);
            object.add("javaArguments", args);
            array.add(object);
        }
        Files.writeString(config.resolve("profiles.json"), new GsonBuilder().setPrettyPrinting().create().toJson(array),
                StandardCharsets.UTF_8);
    }
}
