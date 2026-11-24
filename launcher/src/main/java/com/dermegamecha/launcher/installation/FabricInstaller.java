package com.dermegamecha.launcher.installation;

import com.dermegamecha.launcher.core.DownloadListener;
import com.dermegamecha.launcher.core.DownloadManager;
import com.dermegamecha.launcher.core.LauncherPaths;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FabricInstaller {
    public static final String LOADER_META = "https://meta.fabricmc.net/v2/versions/loader/1.21.1";

    private final LauncherPaths paths;
    private final DownloadManager downloads = new DownloadManager();

    public FabricInstaller(LauncherPaths paths) {
        this.paths = paths;
    }

    public JsonObject install(DownloadListener listener) throws Exception {
        JsonArray loaders = JsonParser.parseString(downloads.getText(LOADER_META)).getAsJsonArray();
        if (loaders.isEmpty()) throw new IllegalStateException("No Fabric loader metadata is available");

        JsonObject selected = null;
        for (var element : loaders) {
            JsonObject candidate = element.getAsJsonObject();
            JsonObject loader = candidate.getAsJsonObject("loader");
            if (loader != null && loader.has("stable") && loader.get("stable").getAsBoolean()) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) selected = loaders.get(0).getAsJsonObject();

        String loaderVersion = selected.getAsJsonObject("loader").get("version").getAsString();
        String profileId = "fabric-loader-" + loaderVersion;
        Path versionDir = paths.root.resolve("versions").resolve(profileId);
        Files.createDirectories(versionDir);

        String profileUrl = "https://meta.fabricmc.net/v2/versions/loader/1.21.1/"
                + loaderVersion + "/profile/json";
        Path profileFile = versionDir.resolve(profileId + ".json");
        downloads.download(profileUrl, profileFile, null, listener);
        JsonObject profile = JsonParser.parseString(
                Files.readString(profileFile, StandardCharsets.UTF_8)).getAsJsonObject();
        selected.getAsJsonObject("loader").addProperty("version", loaderVersion);
        return profile;
    }
}
