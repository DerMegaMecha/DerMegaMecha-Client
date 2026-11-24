package com.dermegamecha.launcher.mods;

import com.dermegamecha.launcher.core.DownloadListener;
import com.dermegamecha.launcher.core.DownloadManager;
import com.dermegamecha.launcher.core.LauncherPaths;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ModManager {
    private record ModSpec(String slug, String name) {}

    private static final ModSpec[] FORGE_DEFAULTS = {
        new ModSpec("embeddium", "Embeddium"),
        new ModSpec("entityculling", "Entity Culling"),
        new ModSpec("appleskin", "AppleSkin"),
        new ModSpec("betterf3", "BetterF3"),
        new ModSpec("mouse-tweaks", "Mouse Tweaks")
    };

    private static final String[] FABRIC_SLUGS = {
        "architectury-api", "clumps", "emi",
        "entityculling", "fabric-api", "ferrite-core", "lithium",
        "sodium-extra", "sodium", "veil", "sable"
    };

    private final LauncherPaths paths;
    private final DownloadManager downloads = new DownloadManager();

    public ModManager(LauncherPaths paths) {
        this.paths = paths;
    }

    public void installDefaults(DownloadListener listener) throws Exception {
        installForgeMods(paths.defaultInstance(), listener);
    }

    public void installForgeMods(Path instance, DownloadListener listener) throws Exception {
        installVersionedMods(instance.resolve("mods"), "forge", FORGE_DEFAULTS, null, listener);
    }

    public void installFabricMods(Path instance, DownloadListener listener) throws Exception {
        ModSpec[] specs = new ModSpec[FABRIC_SLUGS.length];
        for (int i = 0; i < FABRIC_SLUGS.length; i++) {
            specs[i] = new ModSpec(FABRIC_SLUGS[i], FABRIC_SLUGS[i]);
        }
        installVersionedMods(instance.resolve("mods"), "fabric", specs,
                ".dermegmecha-fabric-manifest.json", listener);
    }

    private void installVersionedMods(Path mods, String loader, ModSpec[] specs,
                                      String manifestName, DownloadListener listener) throws Exception {
        Files.createDirectories(mods);
        JsonArray manifest = new JsonArray();
        for (ModSpec spec : specs) {
            String url = "https://api.modrinth.com/v2/project/" + spec.slug()
                    + "/version?loaders=%5B%22" + loader + "%22%5D&game_versions=%5B%221.21.1%22%5D";
            JsonArray versions = JsonParser.parseString(downloads.getText(url)).getAsJsonArray();
            if (versions.isEmpty()) {
                throw new IllegalStateException("No verified " + loader
                        + " 1.21.1 build found for " + spec.name());
            }

            JsonObject selected = null;
            for (JsonElement element : versions) {
                JsonObject candidate = element.getAsJsonObject();
                if (candidate.has("status") && !"listed".equals(candidate.get("status").getAsString())) continue;
                selected = candidate;
                break;
            }
            if (selected == null) throw new IllegalStateException("No listed " + loader + " build found for " + spec.name());

            JsonObject primary = null;
            for (JsonElement fileElement : selected.getAsJsonArray("files")) {
                JsonObject candidate = fileElement.getAsJsonObject();
                if (!candidate.has("primary") || candidate.get("primary").getAsBoolean()) {
                    primary = candidate;
                    break;
                }
            }
            if (primary == null) throw new IllegalStateException("No primary file found for " + spec.name());

            String filename = primary.get("filename").getAsString();
            Path target = mods.resolve(filename);
            String sha512 = primary.getAsJsonObject("hashes").has("sha512")
                    ? primary.getAsJsonObject("hashes").get("sha512").getAsString() : null;
            downloads.download(primary.get("url").getAsString(), target, null, listener);
            if (sha512 != null) {
                String actual = HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-512").digest(Files.readAllBytes(target)));
                if (!actual.equalsIgnoreCase(sha512)) {
                    Files.deleteIfExists(target);
                    throw new IllegalStateException("SHA-512 mismatch for " + filename);
                }
            }

            JsonObject record = new JsonObject();
            record.addProperty("slug", spec.slug());
            record.addProperty("name", spec.name());
            record.addProperty("filename", filename);
            record.addProperty("source", "Modrinth");
            record.addProperty("sha512", sha512 == null ? "" : sha512);
            manifest.add(record);
        }
        String manifestFile = manifestName == null ? ".dermegmecha-manifest.json" : manifestName;
        Files.writeString(mods.resolve(manifestFile), manifest.toString(), StandardCharsets.UTF_8);
        repairManifest(mods, manifestFile);
        removeExcludedMods(mods);
    }

    public List<ModEntry> list(Path instance) throws Exception {
        Path mods = instance.resolve("mods");
        Path forgeManifest = mods.resolve(".dermegmecha-manifest.json");
        Path fabricManifest = mods.resolve(".dermegmecha-fabric-manifest.json");
        Map<String, ModEntry> entries = new LinkedHashMap<>();

        for (Path manifestFile : List.of(forgeManifest, fabricManifest)) {
            if (!Files.isRegularFile(manifestFile)) continue;
            JsonArray manifest = JsonParser.parseString(
                    Files.readString(manifestFile, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement element : manifest) {
                JsonObject item = element.getAsJsonObject();
                String filename = item.get("filename").getAsString();
                entries.put(filename, new ModEntry(
                        item.get("slug").getAsString(),
                        item.get("name").getAsString(),
                        filename,
                        item.get("source").getAsString(),
                        !filename.endsWith(".disabled")));
            }
        }

        if (Files.isDirectory(mods)) {
            try (var stream = Files.list(mods)) {
                stream.filter(path -> {
                    String name = path.getFileName().toString();
                    return name.endsWith(".jar") || name.endsWith(".jar.disabled");
                }).forEach(path -> {
                    String filename = path.getFileName().toString();
                    String base = filename.endsWith(".disabled")
                            ? filename.substring(0, filename.length() - ".disabled".length()) : filename;
                    if (!entries.containsKey(filename) && !entries.containsKey(base)) {
                        entries.put(filename, new ModEntry(
                                "unknown", filename, filename, "user-managed",
                                !filename.endsWith(".disabled")));
                    }
                });
            }
        }
        return List.copyOf(entries.values());
    }

    public void setEnabled(Path instance, String filename, boolean enabled) throws Exception {
        Path mods = instance.resolve("mods");
        Path source = mods.resolve(filename);
        if (!filename.endsWith(".jar") && !filename.endsWith(".jar.disabled")) {
            throw new IOException("Unsupported mod file");
        }
        Path target = enabled
                ? mods.resolve(filename.endsWith(".disabled")
                    ? filename.substring(0, filename.length() - ".disabled".length()) : filename)
                : mods.resolve(filename.endsWith(".disabled") ? filename : filename + ".disabled");
        if (!Files.exists(source) || source.equals(target)) return;
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void repairManifest(Path mods, String manifestName) throws Exception {
        Path manifestFile = mods.resolve(manifestName);
        if (!Files.isRegularFile(manifestFile)) return;
        JsonArray manifest = JsonParser.parseString(
                Files.readString(manifestFile, StandardCharsets.UTF_8)).getAsJsonArray();
        Set<String> owned = new HashSet<>();
        for (JsonElement element : manifest) {
            owned.add(element.getAsJsonObject().get("filename").getAsString());
        }
        try (var stream = Files.list(mods)) {
            stream.forEach(path -> {
                String name = path.getFileName().toString();
                if (name.endsWith(".jar.disabled") && owned.contains(name.substring(0, name.length() - ".disabled".length()))) {
                    // Ownership check passed; the file is intentionally disabled.
                }
            });
        }
    }

    private void removeExcludedMods(Path mods) throws Exception {
        if (!Files.isDirectory(mods)) return;
        try (var stream = Files.list(mods)) {
            stream.filter(path -> {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.endsWith(".jar") || name.endsWith(".jar.disabled");
            }).filter(path -> {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.contains("praxic") || name.contains("cardboard");
            }).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        }
    }

    public List<String> detectDuplicates(Path instance) throws Exception {
        Map<String, Integer> names = new HashMap<>();
        Path mods = instance.resolve("mods");
        if (!Files.isDirectory(mods)) return List.of();
        try (var stream = Files.list(mods)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        String normalized = name.toLowerCase(Locale.ROOT)
                                .replaceAll("[-_.][0-9].*$", "");
                        names.merge(normalized, 1, Integer::sum);
                    });
        }
        return names.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    public int countInstalled() throws Exception {
        Path manifest = paths.defaultInstance().resolve("mods")
                .resolve(".dermegmecha-manifest.json");
        return Files.isRegularFile(manifest)
                ? JsonParser.parseString(Files.readString(manifest, StandardCharsets.UTF_8))
                    .getAsJsonArray().size()
                : 0;
    }
}
