package com.dermegamecha.launcher.mods;

import com.dermegamecha.launcher.core.DownloadListener;
import com.dermegamecha.launcher.core.DownloadManager;
import com.dermegamecha.launcher.core.LauncherPaths;
import com.google.gson.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class ModManager {
    private record ModSpec(String slug, String name) {}
    private static final ModSpec[] DEFAULTS = {
        new ModSpec("embeddium", "Embeddium"), new ModSpec("entityculling", "Entity Culling"),
        new ModSpec("appleskin", "AppleSkin"), new ModSpec("betterf3", "BetterF3"),
        new ModSpec("mouse-tweaks", "Mouse Tweaks")
    };

    private final LauncherPaths paths;
    private final DownloadManager downloads = new DownloadManager();

    public ModManager(LauncherPaths paths) { this.paths = paths; }

    public void installDefaults(DownloadListener listener) throws Exception {
        Path mods = paths.defaultInstance().resolve("mods");
        Files.createDirectories(mods);
        JsonArray manifest = new JsonArray();
        for (ModSpec spec : DEFAULTS) {
            String url = "https://api.modrinth.com/v2/project/" + spec.slug()
                    + "/version?loaders=%5B%22forge%22%5D&game_versions=%5B%221.21.1%22%5D";
            JsonArray versions = JsonParser.parseString(downloads.getText(url)).getAsJsonArray();
            if (versions.isEmpty()) throw new IllegalStateException("No verified Forge 1.21.1 build found for " + spec.name);
            JsonObject selected = null;
            for (JsonElement element : versions) {
                JsonObject candidate = element.getAsJsonObject();
                if (candidate.has("status") && !"listed".equals(candidate.get("status").getAsString())) continue;
                for (JsonElement fileElement : candidate.getAsJsonArray("files")) {
                    JsonObject file = fileElement.getAsJsonObject();
                    if (file.has("primary") && file.get("primary").getAsBoolean()) { selected = candidate; break; }
                }
                if (selected != null) break;
            }
            if (selected == null) selected = versions.get(0).getAsJsonObject();
            JsonObject primary = null;
            for (JsonElement fileElement : selected.getAsJsonArray("files")) {
                JsonObject candidate = fileElement.getAsJsonObject();
                if (candidate.has("primary") && candidate.get("primary").getAsBoolean()) { primary = candidate; break; }
            }
            if (primary == null) primary = selected.getAsJsonArray("files").get(0).getAsJsonObject();

            String fileName = primary.get("filename").getAsString();
            Path target = mods.resolve(fileName);
            String sha512 = primary.getAsJsonObject("hashes").has("sha512")
                    ? primary.getAsJsonObject("hashes").get("sha512").getAsString() : null;
            downloads.download(primary.get("url").getAsString(), target, null, listener);
            if (sha512 != null) {
                byte[] bytes = Files.readAllBytes(target);
                String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(bytes));
                if (!actual.equalsIgnoreCase(sha512)) { Files.deleteIfExists(target); throw new IllegalStateException("SHA-512 mismatch for " + fileName); }
            }
            JsonObject record = new JsonObject(); record.addProperty("slug", spec.slug); record.addProperty("name", spec.name);
            record.addProperty("filename", fileName); record.addProperty("source", "Modrinth"); record.addProperty("sha512", sha512 == null ? "" : sha512);
            manifest.add(record);
        }
        Files.writeString(mods.resolve(".dermegmecha-manifest.json"), manifest.toString(), StandardCharsets.UTF_8);
        removeDuplicateManagedJars(mods);
    }

    private void removeDuplicateManagedJars(Path mods) throws Exception {
        JsonObject manifest = JsonParser.parseString(Files.readString(mods.resolve(".dermegmecha-manifest.json")).toString()).getAsJsonObject();
    }

    public int countInstalled() throws Exception { Path manifest = paths.defaultInstance().resolve("mods").resolve(".dermegmecha-manifest.json"); return Files.isRegularFile(manifest) ? JsonParser.parseString(Files.readString(manifest)).getAsJsonArray().size() : 0; }
}
