package com.dermegamecha.launcher.integration.modpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ModrinthPackServiceTest {
    @Test
    void validatesSeparatedSupportedForgeAndFabricMetadata(@TempDir Path temp) throws Exception {
        Path forge = createPack(temp.resolve("forge.mrpack"), "1.21.1", "forge");
        Path fabric = createPack(temp.resolve("fabric.mrpack"), "1.21.1", "fabric-loader");

        var forgeInfo = new ModrinthPackService(temp.resolve("cache")).inspect(forge);
        var fabricInfo = new ModrinthPackService(temp.resolve("cache")).inspect(fabric);

        assertEquals("forge", forgeInfo.loader());
        assertEquals("fabric", fabricInfo.loader());
        assertNotEquals(forgeInfo.loader(), fabricInfo.loader());
        assertTrue(ModrinthPackService.matchesServerRequirements(forgeInfo, "1.21.1", "test-pack-1"));
        assertFalse(ModrinthPackService.matchesServerRequirements(forgeInfo, "1.20.1", "test-pack-1"));
        assertFalse(ModrinthPackService.matchesServerRequirements(forgeInfo, "1.21.1", "other-pack"));
    }

    @Test
    void rejectsMaliciousArchivePaths(@TempDir Path temp) throws Exception {
        Path archive = createPack(temp.resolve("unsafe.mrpack"), "1.21.1", "forge", "../outside.txt");
        assertThrows(IOException.class, () -> new ModrinthPackService(temp.resolve("cache")).inspect(archive));
    }

    @Test
    void rejectsUnsupportedMinecraftVersionsAndMissingLoaders(@TempDir Path temp) throws Exception {
        Path unsupported = createPack(temp.resolve("unsupported.mrpack"), "1.20.1", "forge");
        Path noLoader = createPack(temp.resolve("no-loader.mrpack"), "1.21.1", null);

        assertThrows(IOException.class, () -> new ModrinthPackService(temp.resolve("cache")).inspect(unsupported));
        assertThrows(IOException.class, () -> new ModrinthPackService(temp.resolve("cache")).inspect(noLoader));
    }

    @Test
    void rejectsDownloadedFileHashMismatch(@TempDir Path temp) throws Exception {
        byte[] expected = "good".getBytes(StandardCharsets.UTF_8);
        Path archive = createPackWithFile(temp.resolve("bad-hash.mrpack"), expected);
        ModrinthPackService service = new ModrinthPackService(temp.resolve("cache"),
                uri -> new ByteArrayInputStream("evil".getBytes(StandardCharsets.UTF_8)));

        IOException error = assertThrows(IOException.class, () -> service.validateAndVerify(archive, null));
        assertTrue(error.getMessage().contains("Hash or file-size mismatch"));
    }

    @Test
    void rejectsUntrustedAndInsecureDownloadUrls(@TempDir Path temp) throws Exception {
        Path archive = createPack(temp.resolve("pack.mrpack"), "1.21.1", "forge");
        JsonObject manifest = manifest("1.21.1", "forge");
        JsonObject file = fileEntry(new byte[] {1});
        file.getAsJsonArray("downloads").set(0, new com.google.gson.JsonPrimitive("http://cdn.modrinth.com/test.jar"));
        manifest.getAsJsonArray("files").add(file);
        writePack(archive, manifest, List.of());

        assertThrows(IOException.class, () -> new ModrinthPackService(temp.resolve("cache")).inspect(archive));
    }

    @Test
    void skipsFilesMarkedUnsupportedForClients(@TempDir Path temp) throws Exception {
        JsonObject manifest = manifest("1.21.1", "fabric-loader");
        JsonObject file = fileEntry(new byte[] {1});
        JsonObject environment = new JsonObject();
        environment.addProperty("client", "unsupported");
        environment.addProperty("server", "required");
        file.add("env", environment);
        manifest.getAsJsonArray("files").add(file);
        Path archive = temp.resolve("server-only.mrpack");
        writePack(archive, manifest, List.of());

        assertTrue(new ModrinthPackService(temp.resolve("cache")).inspect(archive).files().isEmpty());
    }

    @Test
    void refusesPlatformExecutablesInPackPaths(@TempDir Path temp) throws Exception {
        JsonObject manifest = manifest("1.21.1", "forge");
        JsonObject file = fileEntry(new byte[] {1});
        file.addProperty("path", "config/setup.exe");
        manifest.getAsJsonArray("files").add(file);
        Path archive = temp.resolve("executable.mrpack");
        writePack(archive, manifest, List.of());

        assertThrows(IOException.class, () -> new ModrinthPackService(temp.resolve("cache")).inspect(archive));
    }

    @Test
    void stagesAndSanitizesArchiveBeforeExternalImport(@TempDir Path temp) throws Exception {
        Path source = createPack(temp.resolve("source.mrpack"), "1.21.1", "forge");
        ModrinthPackService service = new ModrinthPackService(temp.resolve("cache"));

        Path staged = service.stageLocalArchive(source);
        Path sanitized = service.sanitizeArchive(staged);

        assertNotEquals(source, sanitized);
        assertTrue(sanitized.getFileName().toString().endsWith(".safe.mrpack"));
        assertEquals("forge", service.inspect(sanitized).loader());
        assertEquals(service.archiveSha512(sanitized), service.archiveSha512(service.sanitizeArchive(staged)));
    }

    @Test
    void replacesACompromisedSanitizedCacheEntry(@TempDir Path temp) throws Exception {
        Path source = createPack(temp.resolve("source.mrpack"), "1.21.1", "forge");
        Path cache = temp.resolve("cache");
        ModrinthPackService service = new ModrinthPackService(cache);
        Path staged = service.stageLocalArchive(source);
        Path poisoned = cache.resolve(service.archiveSha512(staged) + ".safe.mrpack");
        createPack(poisoned, "1.21.1", "fabric-loader");

        assertEquals("forge", service.inspect(service.sanitizeArchive(staged)).loader());
    }

    @Test
    void rejectsMalformedManifest(@TempDir Path temp) throws Exception {
        Path archive = temp.resolve("malformed.mrpack");
        Files.createDirectories(temp);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive), StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("modrinth.index.json"));
            zip.write("{not-json".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertThrows(IOException.class, () -> new ModrinthPackService(temp.resolve("cache")).inspect(archive));
    }

    private static Path createPack(Path archive, String minecraft, String loader) throws Exception {
        return createPack(archive, minecraft, loader, null);
    }

    private static Path createPack(Path archive, String minecraft, String loader, String unsafeEntry) throws Exception {
        JsonObject manifest = manifest(minecraft, loader);
        writePack(archive, manifest, unsafeEntry == null ? List.of() : List.of(unsafeEntry));
        return archive;
    }

    private static Path createPackWithFile(Path archive, byte[] bytes) throws Exception {
        JsonObject manifest = manifest("1.21.1", "forge");
        manifest.getAsJsonArray("files").add(fileEntry(bytes));
        writePack(archive, manifest, List.of());
        return archive;
    }

    private static JsonObject manifest(String minecraft, String loader) {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("formatVersion", 1);
        manifest.addProperty("game", "minecraft");
        manifest.addProperty("versionId", "test-pack-1");
        manifest.addProperty("name", "Test Pack");
        manifest.add("files", new JsonArray());
        JsonObject dependencies = new JsonObject();
        dependencies.addProperty("minecraft", minecraft);
        if (loader != null) dependencies.addProperty(loader, loader.equals("forge") ? "52.0.28" : "0.16.10");
        manifest.add("dependencies", dependencies);
        return manifest;
    }

    private static JsonObject fileEntry(byte[] bytes) throws Exception {
        JsonObject file = new JsonObject();
        file.addProperty("path", "mods/test.jar");
        JsonObject hashes = new JsonObject();
        hashes.addProperty("sha1", digest("SHA-1", bytes));
        hashes.addProperty("sha512", digest("SHA-512", bytes));
        file.add("hashes", hashes);
        file.addProperty("fileSize", bytes.length);
        JsonArray downloads = new JsonArray();
        downloads.add("https://cdn.modrinth.com/test.jar");
        file.add("downloads", downloads);
        return file;
    }

    private static String digest(String algorithm, byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));
    }

    private static void writePack(Path archive, JsonObject manifest, List<String> extraEntries) throws Exception {
        Files.createDirectories(archive.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive), StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("modrinth.index.json"));
            zip.write(manifest.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (String entry : extraEntries) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write(new byte[] {1});
                zip.closeEntry();
            }
        }
    }
}
