package com.dermegamecha.launcher.integration.prism;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class PrismLauncherIntegration {
    private static final String EXECUTABLE = "prismlauncher.exe";
    private static final Duration VERSION_TIMEOUT = Duration.ofSeconds(5);

    public Optional<Path> detect(String configuredPath, Map<String, String> environment) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            try {
                Path configured = Path.of(configuredPath).toAbsolutePath().normalize();
                return Files.isRegularFile(configured) ? Optional.of(configured) : Optional.empty();
            } catch (RuntimeException ex) {
                return Optional.empty();
            }
        }

        List<Path> candidates = new ArrayList<>();
        String localAppData = environment.get("LOCALAPPDATA");
        String programFiles = environment.get("ProgramFiles");
        String programFilesX86 = environment.get("ProgramFiles(x86)");
        if (localAppData != null) {
            candidates.add(Path.of(localAppData, "Programs", "PrismLauncher", EXECUTABLE));
            candidates.add(Path.of(localAppData, "PrismLauncher", EXECUTABLE));
        }
        if (programFiles != null) candidates.add(Path.of(programFiles, "PrismLauncher", EXECUTABLE));
        if (programFilesX86 != null) candidates.add(Path.of(programFilesX86, "PrismLauncher", EXECUTABLE));

        String path = environment.get("PATH");
        if (path != null) {
            for (String directory : path.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                if (!directory.isBlank()) candidates.add(Path.of(directory).resolve(EXECUTABLE));
            }
        }
        return candidates.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .filter(Files::isRegularFile)
                .findFirst();
    }

    public String version(Path executable) throws IOException, InterruptedException {
        Path validated = validateExecutable(executable);
        Process process = new ProcessBuilder(validated.toString(), "--version")
                .redirectErrorStream(true)
                .start();
        boolean finished;
        try {
            finished = process.waitFor(VERSION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw ex;
        }
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Prism did not respond to --version within " + VERSION_TIMEOUT.toSeconds() + " seconds.");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.exitValue() != 0 || output.isBlank()) {
            throw new IOException("Prism did not return a version for --version.");
        }
        return output.lines().findFirst().orElseThrow();
    }

    public List<String> importCommand(Path executable, Path packFile) throws IOException {
        Path prism = validateExecutable(executable);
        if (packFile == null || !Files.isRegularFile(packFile)) {
            throw new IOException("The Modrinth pack file does not exist.");
        }
        return List.of(prism.toString(), "--import", packFile.toAbsolutePath().normalize().toString());
    }

    public List<String> launchCommand(Path executable, String instanceId, String serverAddress) throws IOException {
        Path prism = validateExecutable(executable);
        String id = validateInstanceId(instanceId);
        List<String> command = new ArrayList<>(List.of(prism.toString(), "--launch", id));
        if (serverAddress != null && !serverAddress.isBlank()) {
            command.add("--server");
            command.add(ServerAddressValidator.validate(serverAddress));
        }
        return List.copyOf(command);
    }

    public Process importPack(Path executable, Path packFile) throws IOException {
        return start(importCommand(executable, packFile));
    }

    public List<String> openCommand(Path executable) throws IOException {
        return List.of(validateExecutable(executable).toString());
    }

    public Process open(Path executable) throws IOException {
        return start(openCommand(executable));
    }

    public Process launch(Path executable, String instanceId, String serverAddress) throws IOException {
        return start(launchCommand(executable, instanceId, serverAddress));
    }

    public static String validateInstanceId(String instanceId) throws IOException {
        if (instanceId == null || instanceId.isBlank() || !instanceId.equals(instanceId.trim())
                || instanceId.length() > 100 || instanceId.startsWith("-")
                || instanceId.indexOf('/') >= 0 || instanceId.indexOf('\\') >= 0
                || instanceId.chars().anyMatch(Character::isISOControl)) {
            throw new IOException("Enter the Prism instance folder name shown in Prism's instance folder.");
        }
        return instanceId;
    }

    private Path validateExecutable(Path executable) throws IOException {
        if (executable == null || !Files.isRegularFile(executable)) {
            throw new IOException("Prism Launcher was not found. Install Prism Launcher or choose its executable in Settings.");
        }
        String filename = executable.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!filename.equals(EXECUTABLE)) {
            throw new IOException("Select the Prism Launcher executable (prismlauncher.exe).");
        }
        return executable.toAbsolutePath().normalize();
    }

    private Process start(List<String> command) throws IOException {
        try {
            return new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException ex) {
            throw new IOException("Prism could not be started. Check the selected Prism installation and executable.", ex);
        }
    }
}
