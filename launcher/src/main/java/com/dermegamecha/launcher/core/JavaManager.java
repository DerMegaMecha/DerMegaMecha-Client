package com.dermegamecha.launcher.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;

public final class JavaManager {
    public record JavaRuntime(Path executable, String version, boolean java21) {}

    public JavaRuntime detect() throws Exception {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            Path exe = Path.of(javaHome, "bin", "java.exe");
            if (Files.isRegularFile(exe)) return inspect(exe);
        }
        Path exe = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        if (Files.isRegularFile(exe)) return inspect(exe);
        throw new Exception("No Java executable was found. Install a 64-bit JDK or JRE 21 and retry.");
    }

    public JavaRuntime inspect(Path exe) throws Exception {
        Process process = new ProcessBuilder(exe.toString(), "-version").redirectErrorStream(true).start();
        String text = new String(process.getInputStream().readAllBytes());
        process.waitFor();
        String version = text.lines().findFirst().orElse("Unknown");
        boolean java21 = version.contains("\"21") || text.matches("(?s).*(?:java|openjdk) 21[.].*");
        return new JavaRuntime(exe.toAbsolutePath(), version, java21);
    }
}
