package com.dermegamecha.launcher.core;

import java.nio.file.Path;

public final class LauncherPaths {
    public final Path root;
    public final Path launcher;
    public final Path config;
    public final Path logs;
    public final Path cache;
    public final Path instances;
    public final Path downloads;
    public final Path libraries;

    public LauncherPaths() {
        String appData = System.getenv("APPDATA");
        root = appData == null || appData.isBlank()
                ? Path.of(System.getProperty("user.home"), ".dermegmecha")
                : Path.of(appData, ".dermegmecha");
        launcher = root.resolve("launcher");
        config = launcher.resolve("config");
        logs = launcher.resolve("logs");
        cache = launcher.resolve("cache");
        instances = root.resolve("instances");
        downloads = root.resolve("downloads");
        libraries = root.resolve("libraries");
    }

    public Path defaultInstance() { return instances.resolve("dermegmecha-forge-1.21.1"); }
    public Path fabricInstance() { return instances.resolve("dermegmecha-fabric-1.21.1"); }
}
