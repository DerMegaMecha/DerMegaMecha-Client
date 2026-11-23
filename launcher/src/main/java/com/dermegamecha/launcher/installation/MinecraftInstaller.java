package com.dermegamecha.launcher.installation;

import com.dermegamecha.launcher.core.DownloadListener;
import com.dermegamecha.launcher.core.DownloadManager;
import com.dermegamecha.launcher.core.LauncherPaths;
import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MinecraftInstaller {
    public static final String MINECRAFT_VERSION = "1.21.1";
    private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private final DownloadManager downloads = new DownloadManager();
    private final LauncherPaths paths;

    public MinecraftInstaller(LauncherPaths paths) { this.paths = paths; }

    public JsonObject install(DownloadListener listener) throws Exception {
        Files.createDirectories(paths.defaultInstance());
        Files.createDirectories(paths.downloads);
        String manifestText = downloads.getText(MANIFEST);
        JsonObject manifest = JsonParser.parseString(manifestText).getAsJsonObject();
        JsonObject selected = null;
        for (JsonElement entry : manifest.getAsJsonArray("versions")) {
            JsonObject object = entry.getAsJsonObject();
            if (MINECRAFT_VERSION.equals(object.get("id").getAsString())) { selected = object; break; }
        }
        if (selected == null) throw new IOException("Minecraft " + MINECRAFT_VERSION + " was not found in Mojang's version manifest.");

        Path versionDir = paths.root.resolve("versions").resolve(MINECRAFT_VERSION);
        Files.createDirectories(versionDir);
        Path versionJson = versionDir.resolve(MINECRAFT_VERSION + ".json");
        String versionUrl = selected.get("url").getAsString();
        String versionSha1 = selected.has("sha1") ? selected.get("sha1").getAsString() : null;
        downloads.download(versionUrl, versionJson, versionSha1, listener);
        JsonObject version = JsonParser.parseString(Files.readString(versionJson, StandardCharsets.UTF_8)).getAsJsonObject();

        JsonObject downloadsObject = version.getAsJsonObject("downloads");
        JsonObject client = downloadsObject.getAsJsonObject("client");
        downloads.download(client.get("url").getAsString(), versionDir.resolve(MINECRAFT_VERSION + ".jar"),
                client.has("sha1") ? client.get("sha1").getAsString() : null, listener);

        if (version.has("libraries")) for (JsonElement element : version.getAsJsonArray("libraries")) {
            JsonObject library = element.getAsJsonObject();
            if (!libraryApplies(library)) continue;
            JsonObject downloadsLib = library.getAsJsonObject("downloads");
            if (downloadsLib.has("artifact")) {
                JsonObject artifact = downloadsLib.getAsJsonObject("artifact");
                Path target = libraryPath(paths.libraries, library, artifact);
                downloads.download(artifact.get("url").getAsString(), target,
                        artifact.has("sha1") ? artifact.get("sha1").getAsString() : null, listener);
            }
        }

        JsonObject assetIndex = version.getAsJsonObject("assetIndex");
        Path indexes = paths.root.resolve("assets").resolve("indexes");
        Files.createDirectories(indexes);
        Path indexFile = indexes.resolve(assetIndex.get("id").getAsString() + ".json");
        downloads.download(assetIndex.get("url").getAsString(), indexFile,
                assetIndex.has("sha1") ? assetIndex.get("sha1").getAsString() : null, listener);
        JsonObject index = JsonParser.parseString(Files.readString(indexFile, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject objects = index.getAsJsonObject("objects");
        Path assets = paths.root.resolve("assets").resolve("objects");
        for (var entry : objects.entrySet()) {
            JsonObject object = entry.getValue().getAsJsonObject();
            String hash = object.get("hash").getAsString();
            String prefix = hash.substring(0, 2);
            downloads.download("https://resources.download.minecraft.net/" + prefix + "/" + hash,
                    assets.resolve(prefix).resolve(hash), hash, listener);
        }

        validate(versionDir, versionJson, version);
        return version;
    }

    private boolean libraryApplies(JsonObject library) {
        if (!library.has("rules")) return true;
        boolean allow = false;
        for (JsonElement ruleElement : library.getAsJsonArray("rules")) {
            JsonObject rule = ruleElement.getAsJsonObject();
            if (rule.has("os") && !rule.getAsJsonObject("os").get("name").getAsString().equals("windows")) continue;
            allow = "allow".equals(rule.get("action").getAsString()) || allow;
        }
        return allow;
    }

    private Path libraryPath(Path root, JsonObject library, JsonObject artifact) {
        if (artifact.has("path") && !artifact.get("path").getAsString().isBlank())
            return root.resolve(artifact.get("path").getAsString().replace('/', java.io.File.separatorChar));
        String name = library.get("name").getAsString();
        String[] parts = name.split(":");
        if (parts.length < 3) throw new IllegalArgumentException("Invalid library name: " + name);
        String artifactName = parts[1] + "-" + parts[2] + ".jar";
        return root.resolve(parts[0].replace('.', java.io.File.separatorChar)).resolve(parts[1]).resolve(parts[2]).resolve(artifactName);
    }

    private void validate(Path versionDir, Path versionJson, JsonObject version) throws IOException {
        if (!Files.isRegularFile(versionJson) || !Files.isRegularFile(versionDir.resolve(MINECRAFT_VERSION + ".jar")))
            throw new IOException("Minecraft installation is incomplete.");
        if (!version.has("libraries") || !version.has("assetIndex"))
            throw new IOException("Downloaded Minecraft metadata is invalid.");
    }
}
