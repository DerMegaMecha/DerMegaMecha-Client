package com.dermegamecha.launcher.installation;

import com.dermegamecha.launcher.core.DownloadListener;
import com.dermegamecha.launcher.core.DownloadManager;
import com.dermegamecha.launcher.core.LauncherPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ForgeInstaller {
    public static final String FORGE = "52.0.28";
    public static final String FORGE_ID = "1.21.1-forge-" + FORGE;
    private final LauncherPaths paths;
    private final DownloadManager downloads = new DownloadManager();

    public ForgeInstaller(LauncherPaths paths) { this.paths = paths; }

    public void install(Path javaExecutable, DownloadListener listener) throws Exception {
        Path versionDir = paths.root.resolve("versions").resolve(FORGE_ID);
        Files.createDirectories(paths.downloads);
        Files.createDirectories(versionDir);
        if (Files.isRegularFile(versionDir.resolve(FORGE_ID + ".json"))) return;

        String url = "https://maven.minecraftforge.net/net/minecraftforge/forge/1.21.1-" + FORGE
                + "/forge-1.21.1-" + FORGE + "-installer.jar";
        Path installer = paths.downloads.resolve("forge-1.21.1-" + FORGE + "-installer.jar");
        downloads.download(url, installer, null, listener);

        Process process = new ProcessBuilder(javaExecutable.toString(), "-jar", installer.toString(),
                "--installClient", paths.defaultInstance().toString())
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        int result = process.waitFor();
        if (result != 0) throw new IOException("Forge installer failed with exit code " + result + ": " + summarize(output));

        Path json = versionDir.resolve(FORGE_ID + ".json");
        Path installedJson = paths.defaultInstance().resolve("versions").resolve(FORGE_ID).resolve(FORGE_ID + ".json");
        if (Files.isRegularFile(installedJson) && !Files.isRegularFile(json)) Files.copy(installedJson, json);
        if (!Files.isRegularFile(json)) {
            Path launcherVersions = paths.root.resolve("versions").resolve(FORGE_ID).resolve(FORGE_ID + ".json");
            if (!Files.isRegularFile(launcherVersions)) throw new IOException("Forge installer completed but generated no launch metadata: " + summarize(output));
        }
    }

    private String summarize(String output) {
        String clean = output.replaceAll("(?i)(access_token|refresh_token|password)=[^\\s]+", "$1=<redacted>");
        if (clean.length() > 2000) return clean.substring(clean.length() - 2000);
        return clean.strip();
    }
}
