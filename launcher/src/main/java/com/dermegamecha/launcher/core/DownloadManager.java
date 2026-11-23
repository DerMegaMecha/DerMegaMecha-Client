package com.dermegamecha.launcher.core;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

public final class DownloadManager {
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30)).build();

    public void download(String url, Path target, String expectedSha1, DownloadListener listener) throws Exception {
        Files.createDirectories(target.getParent());
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(20)).GET().build();
        HttpResponse<java.io.InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new java.io.IOException("HTTP " + response.statusCode() + " downloading " + url);
        long length = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        MessageDigest sha1 = expectedSha1 == null ? null : MessageDigest.getInstance("SHA-1");
        long downloaded = 0;
        long start = System.nanoTime();
        try (var in = response.body(); var channel = FileChannel.open(target,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                channel.write(ByteBuffer.wrap(buffer, 0, read));
                if (sha1 != null) sha1.update(buffer, 0, read);
                downloaded += read;
                if (listener != null) {
                    double seconds = Math.max(0.001, (System.nanoTime() - start) / 1e9);
                    listener.update(new DownloadProgress("Downloading", target.getFileName().toString(), downloaded, length, 0, 1, downloaded / seconds));
                }
            }
        }
        if (expectedSha1 != null) {
            String actual = HexFormat.of().formatHex(sha1.digest());
            if (!actual.equalsIgnoreCase(expectedSha1)) {
                Files.deleteIfExists(target);
                throw new java.io.IOException("SHA-1 mismatch for " + target.getFileName() + ": expected " + expectedSha1 + ", got " + actual);
            }
        }
    }

    public String getText(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new java.io.IOException("HTTP " + response.statusCode() + " fetching " + url);
        return response.body();
    }
}
