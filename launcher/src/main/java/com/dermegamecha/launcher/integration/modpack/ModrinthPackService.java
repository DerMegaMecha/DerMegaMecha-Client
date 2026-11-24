package com.dermegamecha.launcher.integration.modpack;

import com.dermegamecha.launcher.core.DownloadListener;
import com.dermegamecha.launcher.core.DownloadProgress;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class ModrinthPackService {
    public static final String SUPPORTED_MINECRAFT_VERSION = "1.21.1";
    private static final long MAX_ARCHIVE_BYTES = 200L * 1024 * 1024;
    private static final long MAX_UNCOMPRESSED_ARCHIVE_BYTES = 1024L * 1024 * 1024;
    private static final long MAX_FILE_BYTES = 512L * 1024 * 1024;
    private static final long MAX_TOTAL_FILE_BYTES = 2L * 1024 * 1024 * 1024;
    private static final int MAX_ARCHIVE_ENTRIES = 20_000;
    private static final int MAX_MANIFEST_BYTES = 2 * 1024 * 1024;
    private static final Set<String> DOWNLOAD_HOSTS = Set.of(
            "cdn.modrinth.com", "modrinth.com", "api.modrinth.com",
            "github.com", "raw.githubusercontent.com", "release-assets.githubusercontent.com", "gitlab.com");

    @FunctionalInterface
    interface RemoteFileSource {
        InputStream open(URI uri) throws IOException;
    }

    public record PackFile(String path, String sha1, String sha512, long size, List<URI> downloads) {}
    public record PackInfo(String name, String versionId, String minecraftVersion,
                           String loader, String loaderVersion, List<PackFile> files) {}

    public static boolean matchesServerRequirements(PackInfo pack, String minecraftVersion, String packVersion) {
        return pack != null && minecraftVersion != null && packVersion != null
                && pack.minecraftVersion().equals(minecraftVersion.trim())
                && pack.versionId().equals(packVersion.trim());
    }

    private final Path cache;
    private final RemoteFileSource remoteFileSource;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public ModrinthPackService(Path cache) {
        this(cache, null);
    }

    public static String configuredPackUrl() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = ModrinthPackService.class.getResourceAsStream("/dermegamecha-launcher.properties")) {
            if (input == null) return "";
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            return properties.getProperty("modpackUrl", "").trim();
        }
    }

    ModrinthPackService(Path cache, RemoteFileSource remoteFileSource) {
        this.cache = cache.toAbsolutePath().normalize();
        this.remoteFileSource = remoteFileSource;
    }

    public Path downloadAndVerify(URI packUri, DownloadListener listener) throws Exception {
        validateDownloadUri(packUri);
        Files.createDirectories(cache);
        Path archive = cache.resolve("dermegamecha-modpack.mrpack");
        Path temporary = Files.createTempFile(cache, "modpack-", ".download");
        try {
            download(packUri, temporary, MAX_ARCHIVE_BYTES, listener, "Downloading modpack");
            try (var zip = new ZipFile(temporary.toFile(), StandardCharsets.UTF_8)) {
                inspect(zip);
            }
            Files.move(temporary, archive, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
        verifyReferencedFiles(archive, listener);
        return archive;
    }

    public Path stageLocalArchive(Path source) throws Exception {
        if (source == null || !Files.isRegularFile(source) || Files.size(source) > MAX_ARCHIVE_BYTES) {
            throw new IOException("Choose a valid Modrinth modpack archive within the allowed size.");
        }
        Files.createDirectories(cache);
        String expectedHash = archiveSha512(source);
        Path staged = cache.resolve(expectedHash + ".mrpack");
        if (Files.isRegularFile(staged) && archiveSha512(staged).equals(expectedHash)) return staged;

        Path temporary = Files.createTempFile(cache, "local-pack-", ".mrpack");
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            if (!archiveSha512(temporary).equals(expectedHash)) {
                throw new IOException("The selected modpack changed while it was being copied.");
            }
            Files.move(temporary, staged, StandardCopyOption.REPLACE_EXISTING);
            return staged;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public Path sanitizeArchive(Path source) throws Exception {
        Files.createDirectories(cache);
        inspect(source);
        String archiveHash = archiveSha512(source);
        Path sanitized = cache.resolve(archiveHash + ".safe.mrpack");
        Path temporary = Files.createTempFile(cache, "safe-pack-", ".mrpack");
        boolean moved = false;
        try {
            try (ZipFile input = new ZipFile(source.toFile(), StandardCharsets.UTF_8);
                 OutputStream fileOutput = Files.newOutputStream(temporary);
                 ZipOutputStream zipOutput = new ZipOutputStream(
                         new BoundedOutputStream(fileOutput, MAX_ARCHIVE_BYTES), StandardCharsets.UTF_8)) {
                var entries = input.entries();
                byte[] buffer = new byte[64 * 1024];
                long total = 0;
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    validateArchivePath(entry.getName());
                    ZipEntry sanitizedEntry = new ZipEntry(entry.getName());
                    sanitizedEntry.setTime(0L);
                    zipOutput.putNextEntry(sanitizedEntry);
                    if (!entry.isDirectory()) {
                        try (InputStream entryInput = input.getInputStream(entry)) {
                            int read;
                            long entryTotal = 0;
                            while ((read = entryInput.read(buffer)) != -1) {
                                entryTotal += read;
                                total += read;
                                if (entryTotal > MAX_ARCHIVE_BYTES || total > MAX_UNCOMPRESSED_ARCHIVE_BYTES) {
                                    throw new IOException("The modpack archive expands beyond the allowed size.");
                                }
                                zipOutput.write(buffer, 0, read);
                            }
                        }
                    }
                    zipOutput.closeEntry();
                }
            }
            Files.move(temporary, sanitized, StandardCopyOption.REPLACE_EXISTING);
            moved = true;
        } finally {
            if (!moved) Files.deleteIfExists(temporary);
        }
        inspect(sanitized);
        return sanitized;
    }

    public PackInfo validateAndVerify(Path archive, DownloadListener listener) throws Exception {
        PackInfo info = inspect(archive);
        verifyFiles(info, listener);
        return info;
    }

    public PackInfo inspect(Path archive) throws Exception {
        if (archive == null) throw new IOException("Choose a Modrinth modpack archive.");
        if (!Files.isRegularFile(archive) || Files.size(archive) > MAX_ARCHIVE_BYTES) {
            throw new IOException("The modpack archive is missing or exceeds the allowed size.");
        }
        try (var zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            return inspect(zip);
        }
    }

    public String archiveSha512(Path archive) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        try (InputStream input = Files.newInputStream(archive)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public PackInfo verifyReferencedFiles(Path archive, DownloadListener listener) throws Exception {
        PackInfo info = inspect(archive);
        verifyFiles(info, listener);
        return info;
    }

    private PackInfo inspect(ZipFile zip) throws Exception {
        if (zip.size() > MAX_ARCHIVE_ENTRIES) throw new IOException("The modpack contains too many archive entries.");
        ZipEntry manifestEntry = null;
        TreeSet<String> archivePaths = new TreeSet<>();
        Map<String, Boolean> archivePathKinds = new java.util.HashMap<>();
        long uncompressedBytes = 0;
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            validateArchivePath(entry.getName());
            String normalizedEntry = entry.getName().endsWith("/")
                    ? entry.getName().substring(0, entry.getName().length() - 1) : entry.getName();
            String normalizedLower = normalizedEntry.toLowerCase(Locale.ROOT);
            if (!archivePaths.add(normalizedLower)) {
                throw new IOException("The modpack archive contains duplicate paths.");
            }
            String[] pathParts = normalizedLower.split("/");
            String parent = "";
            for (int i = 0; i < pathParts.length - 1; i++) {
                parent = parent.isEmpty() ? pathParts[i] : parent + "/" + pathParts[i];
                if (Boolean.FALSE.equals(archivePathKinds.get(parent))) {
                    throw new IOException("The modpack archive contains conflicting file and directory paths.");
                }
            }
            boolean directory = entry.isDirectory();
            String prefix = normalizedLower + "/";
            if (!directory) {
                String descendant = archivePaths.ceiling(prefix);
                if (descendant != null && descendant.startsWith(prefix)) {
                    throw new IOException("The modpack archive contains conflicting file and directory paths.");
                }
            }
            archivePathKinds.put(normalizedLower, directory);
            if (entry.getName().equals("modrinth.index.json")) {
                if (manifestEntry != null || entry.isDirectory()) throw new IOException("The modpack manifest is duplicated or invalid.");
                manifestEntry = entry;
            }
            if (entry.getSize() > MAX_ARCHIVE_BYTES) throw new IOException("A modpack archive entry exceeds the allowed size.");
            if (!entry.isDirectory()) {
                if (entry.getSize() < 0) throw new IOException("The modpack archive contains an entry with unknown size.");
                uncompressedBytes = Math.addExact(uncompressedBytes, entry.getSize());
                if (uncompressedBytes > MAX_UNCOMPRESSED_ARCHIVE_BYTES) {
                    throw new IOException("The modpack archive expands beyond the allowed size.");
                }
            }
        }
        if (manifestEntry == null || manifestEntry.getSize() > MAX_MANIFEST_BYTES) {
            throw new IOException("The archive does not contain a valid Modrinth modpack manifest.");
        }

        JsonObject manifest;
        try (InputStream input = zip.getInputStream(manifestEntry)) {
            byte[] data = input.readNBytes(MAX_MANIFEST_BYTES + 1);
            if (data.length > MAX_MANIFEST_BYTES) throw new IOException("The modpack manifest is too large.");
            manifest = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException ex) {
            throw new IOException("The Modrinth manifest is not valid JSON.", ex);
        }

        int formatVersion;
        try {
            formatVersion = manifest.getAsJsonPrimitive("formatVersion").getAsBigDecimal().intValueExact();
        } catch (RuntimeException ex) {
            throw new IOException("Only Modrinth Minecraft modpack format version 1 is supported.", ex);
        }
        if (!manifest.has("formatVersion") || !manifest.get("formatVersion").isJsonPrimitive()
                || !manifest.getAsJsonPrimitive("formatVersion").isNumber()
                || formatVersion != 1
                || !manifest.has("game") || !manifest.get("game").isJsonPrimitive()
                || !manifest.getAsJsonPrimitive("game").isString()
                || !"minecraft".equals(manifest.get("game").getAsString())) {
            throw new IOException("Only Modrinth Minecraft modpack format version 1 is supported.");
        }
        String name = requiredText(manifest, "name");
        String versionId = requiredText(manifest, "versionId");
        JsonObject dependencies = object(manifest, "dependencies");
        String minecraftVersion = dependency(dependencies, "minecraft");
        if (!SUPPORTED_MINECRAFT_VERSION.equals(minecraftVersion)) {
            throw new IOException("This launcher currently supports Minecraft " + SUPPORTED_MINECRAFT_VERSION
                    + " modpacks; this pack requires " + minecraftVersion + ".");
        }

        String loader;
        String loaderVersion;
        boolean hasForge = dependencies.has("forge");
        boolean hasFabric = dependencies.has("fabric-loader");
        if (dependencies.has("neoforge") || dependencies.has("quilt-loader") || hasForge == hasFabric) {
            throw new IOException("The modpack must declare exactly one supported loader: Forge or Fabric.");
        } else if (hasForge) {
            loader = "forge";
            loaderVersion = dependency(dependencies, loader);
        } else {
            loader = "fabric";
            loaderVersion = dependency(dependencies, "fabric-loader");
        }

        List<PackFile> files = parseFiles(manifest);
        validateOverridePaths(zip);
        return new PackInfo(name, versionId, minecraftVersion, loader, loaderVersion, List.copyOf(files));
    }

    private List<PackFile> parseFiles(JsonObject manifest) throws Exception {
        if (!manifest.has("files") || !manifest.get("files").isJsonArray()) {
            throw new IOException("The modpack manifest has no files list.");
        }
        List<PackFile> result = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        long totalBytes = 0;
        for (JsonElement element : manifest.getAsJsonArray("files")) {
            if (!element.isJsonObject()) throw new IOException("The modpack contains an invalid file entry.");
            JsonObject file = element.getAsJsonObject();
            String path = requiredText(file, "path");
            validatePackPath(path);
            if (!paths.add(path.toLowerCase(Locale.ROOT))) throw new IOException("The modpack contains duplicate file paths.");

            boolean clientUnsupported = false;
            if (file.has("env") && file.get("env").isJsonObject()) {
                JsonObject env = file.getAsJsonObject("env");
                for (String key : List.of("client", "server")) {
                    if (env.has(key)) {
                        if (!env.get(key).isJsonPrimitive() || !env.getAsJsonPrimitive(key).isString()) {
                            throw new IOException("The modpack contains an invalid client/server environment value.");
                        }
                        String value = env.get(key).getAsString();
                        if (!Set.of("required", "optional", "unsupported").contains(value)) {
                            throw new IOException("The modpack contains an invalid client/server environment value.");
                        }
                        if (key.equals("client") && value.equals("unsupported")) clientUnsupported = true;
                    }
                }
            } else if (file.has("env")) {
                throw new IOException("The modpack contains invalid environment metadata.");
            }

            long size = requiredLong(file, "fileSize");
            if (size < 0 || size > 0xffff_ffffL) throw new IOException("The modpack contains an invalid file size.");
            if (!clientUnsupported) {
                if (size > MAX_FILE_BYTES) throw new IOException("A modpack file exceeds the allowed size.");
                totalBytes = Math.addExact(totalBytes, size);
                if (totalBytes > MAX_TOTAL_FILE_BYTES) throw new IOException("The modpack download total exceeds the allowed size.");
            }

            JsonObject hashes = object(file, "hashes");
            String sha1 = requiredHash(hashes, "sha1", 40);
            String sha512 = requiredHash(hashes, "sha512", 128);
            List<URI> urls = new ArrayList<>();
            if (!file.has("downloads") || !file.get("downloads").isJsonArray()
                    || file.getAsJsonArray("downloads").isEmpty()) {
                throw new IOException("A modpack file has no download URL.");
            }
            for (JsonElement download : file.getAsJsonArray("downloads")) {
                if (!download.isJsonPrimitive() || !download.getAsJsonPrimitive().isString()) {
                    throw new IOException("The modpack contains an invalid download URL.");
                }
                URI uri;
                try {
                    uri = URI.create(download.getAsString());
                } catch (IllegalArgumentException ex) {
                    throw new IOException("The modpack contains an invalid download URL.", ex);
                }
                validateDownloadUri(uri);
                urls.add(uri);
            }

            if (!clientUnsupported) result.add(new PackFile(path, sha1, sha512, size, List.copyOf(urls)));
        }
        return result;
    }

    private void validateOverridePaths(ZipFile zip) throws IOException {
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            for (String prefix : List.of("overrides/", "client-overrides/", "server-overrides/")) {
                if (name.startsWith(prefix)) {
                    String relative = name.substring(prefix.length());
                    if (!relative.isEmpty()) validatePackPath(relative);
                }
            }
        }
    }

    private void verifyFiles(PackInfo info, DownloadListener listener) throws Exception {
        Files.createDirectories(cache);
        long total = info.files().stream().mapToLong(PackFile::size).sum();
        long verified = 0;
        int completed = 0;
        for (PackFile file : info.files()) {
            Path cached = cache.resolve(file.sha512() + ".verified");
            if (Files.isRegularFile(cached) && verifyLocal(cached, file)) {
                verified += file.size();
                completed++;
                notifyProgress(listener, "Verifying modpack files", file.path(), verified, total, completed, info.files().size());
                continue;
            }
            Files.deleteIfExists(cached);
            IOException lastError = null;
            for (URI url : file.downloads()) {
                Path temporary = Files.createTempFile(cache, "modfile-", ".download");
                try {
                    download(url, temporary, file.size(), listener, "Verifying modpack files");
                    if (!verifyLocal(temporary, file)) throw new IOException("Hash or file-size mismatch for " + file.path());
                    Files.move(temporary, cached, StandardCopyOption.REPLACE_EXISTING);
                    lastError = null;
                    break;
                } catch (IOException ex) {
                    lastError = ex;
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
            if (lastError != null) throw lastError;
            verified += file.size();
            completed++;
            notifyProgress(listener, "Verifying modpack files", file.path(), verified, total, completed, info.files().size());
        }
    }

    private boolean verifyLocal(Path file, PackFile expected) throws Exception {
        if (Files.size(file) != expected.size()) return false;
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
        try (InputStream input = Files.newInputStream(file);
             DigestInputStream sha1Input = new DigestInputStream(input, sha1)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = sha1Input.read(buffer)) != -1) sha512.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(sha1.digest()).equalsIgnoreCase(expected.sha1())
                && HexFormat.of().formatHex(sha512.digest()).equalsIgnoreCase(expected.sha512());
    }

    private void download(URI uri, Path target, long maxBytes, DownloadListener listener, String phase) throws Exception {
        URI current = uri;
        for (int redirects = 0; redirects <= 3; redirects++) {
            validateDownloadUri(current);
            HttpRequest request = HttpRequest.newBuilder(current).timeout(Duration.ofMinutes(10)).GET().build();
            if (remoteFileSource != null) {
                try (InputStream input = remoteFileSource.open(current)) {
                    copy(input, target, maxBytes, listener, phase, current.toString(), -1);
                }
                return;
            }
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                String location = response.headers().firstValue("Location")
                        .orElseThrow(() -> new IOException("The modpack download redirect has no destination."));
                response.body().close();
                current = current.resolve(location);
                continue;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new IOException("The modpack download failed with HTTP " + response.statusCode() + ".");
            }
            try (InputStream input = response.body()) {
                long length = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (length > maxBytes) throw new IOException("The modpack download exceeds the allowed size.");
                copy(input, target, maxBytes, listener, phase, current.getPath(), length);
            }
            return;
        }
        throw new IOException("The modpack download exceeded the redirect limit.");
    }

    private void copy(InputStream input, Path target, long maxBytes, DownloadListener listener,
                      String phase, String filename, long total) throws IOException {
        long downloaded = 0;
        long started = System.nanoTime();
        try (var output = Files.newOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                downloaded += count;
                if (downloaded > maxBytes) throw new IOException("The modpack download exceeds the allowed size.");
                output.write(buffer, 0, count);
                double seconds = Math.max(0.001, (System.nanoTime() - started) / 1_000_000_000.0);
                notifyProgress(listener, phase, filename, downloaded, total, 0, 1, downloaded / seconds);
            }
        }
    }

    private static void notifyProgress(DownloadListener listener, String phase, String file, long downloaded,
                                       long total, int completed, int fileCount) {
        notifyProgress(listener, phase, file, downloaded, total, completed, fileCount, 0);
    }

    private static void notifyProgress(DownloadListener listener, String phase, String file, long downloaded,
                                       long total, int completed, int fileCount, double speed) {
        if (listener != null) {
            String name = file == null || file.isBlank() ? "modpack" : file;
            int separator = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
            if (separator >= 0) name = name.substring(separator + 1);
            listener.update(new DownloadProgress(phase, name, downloaded, total, completed, fileCount, speed));
        }
    }

    private static String requiredText(JsonObject object, String key) throws IOException {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isString()
                || object.get(key).getAsString().isBlank()) {
            throw new IOException("The modpack manifest is missing " + key + ".");
        }
        return object.get(key).getAsString();
    }

    private static long requiredLong(JsonObject object, String key) throws IOException {
        try {
            if (!object.has(key) || !object.get(key).isJsonPrimitive()
                    || !object.getAsJsonPrimitive(key).isNumber()) {
                throw new NumberFormatException("not a JSON number");
            }
            return object.getAsJsonPrimitive(key).getAsBigDecimal().longValueExact();
        } catch (RuntimeException ex) {
            throw new IOException("The modpack manifest contains an invalid " + key + ".", ex);
        }
    }

    private static JsonObject object(JsonObject object, String key) throws IOException {
        if (!object.has(key) || !object.get(key).isJsonObject()) {
            throw new IOException("The modpack manifest is missing " + key + ".");
        }
        return object.getAsJsonObject(key);
    }

    private static String dependency(JsonObject dependencies, String key) throws IOException {
        if (!dependencies.has(key) || !dependencies.get(key).isJsonPrimitive()
                || !dependencies.getAsJsonPrimitive(key).isString()
                || dependencies.get(key).getAsString().isBlank()) {
            throw new IOException("The modpack does not declare a valid " + key + " version.");
        }
        return dependencies.get(key).getAsString();
    }

    private static String requiredHash(JsonObject hashes, String key, int length) throws IOException {
        String value = dependency(hashes, key);
        if (value.length() != length || !value.matches("(?i)[0-9a-f]+")) {
            throw new IOException("The modpack contains an invalid " + key + " hash.");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static void validateDownloadUri(URI uri) throws IOException {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || !DOWNLOAD_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT))
                || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IOException("The modpack contains a download URL from an unsupported or insecure source.");
        }
    }

    private static final class BoundedOutputStream extends FilterOutputStream {
        private final long maximum;
        private long written;

        private BoundedOutputStream(OutputStream output, long maximum) {
            super(output);
            this.maximum = maximum;
        }

        @Override public void write(int value) throws IOException {
            ensureCapacity(1);
            out.write(value);
            written++;
        }

        @Override public void write(byte[] buffer, int offset, int length) throws IOException {
            ensureCapacity(length);
            out.write(buffer, offset, length);
            written += length;
        }

        private void ensureCapacity(int length) throws IOException {
            if (length < 0 || written + length > maximum) {
                throw new IOException("The sanitized modpack archive exceeds the allowed size.");
            }
        }
    }

    private static void validateArchivePath(String name) throws IOException {
        String normalized = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
        validatePackPath(normalized);
    }

    private static void validatePackPath(String path) throws IOException {
        if (path == null || path.isBlank() || path.startsWith("/") || path.startsWith("\\")
                || path.length() > 4096 || path.contains("\\") || path.contains(":")
                || path.chars().anyMatch(Character::isISOControl) || path.matches("(?i)^[a-z]:.*")) {
            throw new IOException("The modpack contains an unsafe archive path.");
        }
        for (String part : path.split("/", -1)) {
            if (part.isBlank() || part.length() > 255 || part.equals(".") || part.equals("..")) {
                throw new IOException("The modpack contains an unsafe archive path.");
            }
            String deviceName = part.split("\\.", 2)[0].toUpperCase(Locale.ROOT);
            String normalizedDeviceName = deviceName.replaceAll("[ .]+$", "");
            if (part.endsWith(" ") || part.endsWith(".")
                    || Set.of("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4",
                    "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3",
                    "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9").contains(normalizedDeviceName)) {
                throw new IOException("The modpack contains an unsafe archive path.");
            }
        }
        String lowerPath = path.toLowerCase(Locale.ROOT);
        if (List.of(".exe", ".msi", ".bat", ".cmd", ".ps1", ".com", ".scr", ".dll", ".sh", ".vbs")
                .stream().anyMatch(lowerPath::endsWith)) {
            throw new IOException("The modpack contains a platform executable or script, which is not supported.");
        }
    }
}
