package com.dermegamecha.launcher.launch;

import com.dermegamecha.launcher.auth.SignedInAccount;
import com.dermegamecha.launcher.core.LauncherPaths;
import com.dermegamecha.launcher.installation.ForgeInstaller;
import com.google.gson.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class MinecraftLauncher {
    private final LauncherPaths paths;

    public MinecraftLauncher(LauncherPaths paths) { this.paths = paths; }

    public Process launch(SignedInAccount account, Path javaExecutable, int ramMb, boolean fullscreen,
                         int width, int height) throws Exception {
        if (account == null || account.accessToken() == null) throw new IllegalStateException("A valid Microsoft Minecraft session is required.");
        Path instance = paths.defaultInstance();
        Path forgeDir = paths.root.resolve("versions").resolve(ForgeInstaller.FORGE_ID);
        Path forgeJson = forgeDir.resolve(ForgeInstaller.FORGE_ID + ".json");
        if (!Files.isRegularFile(forgeJson)) throw new IllegalStateException("Forge launch metadata is missing. Run repair first.");
        JsonObject forge = JsonParser.parseString(Files.readString(forgeJson, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject vanilla = JsonParser.parseString(Files.readString(paths.root.resolve("versions").resolve("1.21.1").resolve("1.21.1.json"), StandardCharsets.UTF_8)).getAsJsonObject();

        List<String> command = new ArrayList<>();
        command.add(javaExecutable.toString());
        command.add("-Xms" + ramMb + "m"); command.add("-Xmx" + ramMb + "m");
        command.add("-Djava.library.path=" + instance.resolve("natives").toAbsolutePath());
        command.add("-Dminecraft.launcher.brand=dermegamecha");
        command.add("-Dminecraft.launcher.version=1.0.0");
        command.add("-cp");

        Set<String> classpath = new LinkedHashSet<>();
        addLibraries(classpath, vanilla);
        addLibraries(classpath, forge);
        classpath.add(forgeDir.resolve(ForgeInstaller.FORGE_ID + ".jar").toString());
        classpath.add(paths.root.resolve("versions").resolve("1.21.1").resolve("1.21.1.jar").toString());
        command.add(String.join(java.io.File.pathSeparator, classpath));

        String mainClass = forge.has("mainClass") ? forge.get("mainClass").getAsString()
                : (vanilla.has("mainClass") ? vanilla.get("mainClass").getAsString() : "cpw.mods.bootstraplauncher.BootstrapLauncher");
        command.add(mainClass);

        Map<String, String> args = new HashMap<>();
        args.put("auth_player_name", account.name()); args.put("version_name", ForgeInstaller.FORGE_ID);
        args.put("game_directory", instance.toAbsolutePath().toString()); args.put("assets_root", paths.root.resolve("assets").toAbsolutePath().toString());
        args.put("assets_index_name", "16"); args.put("auth_uuid", account.id()); args.put("auth_access_token", account.accessToken());
        args.put("user_type", "msa"); args.put("version_type", "release"); args.put("clientid", "dermegamecha-launcher"); args.put("auth_xuid", "0");

        if (forge.has("arguments") && forge.getAsJsonObject("arguments").has("game")) {
            appendArguments(command, forge.getAsJsonObject("arguments").getAsJsonArray("game"), args);
        } else if (vanilla.has("arguments") && vanilla.getAsJsonObject("arguments").has("game")) {
            appendArguments(command, vanilla.getAsJsonObject("arguments").getAsJsonArray("game"), args);
        }
        if (fullscreen) command.add("--fullscreen"); else { command.add("--width"); command.add(Integer.toString(width)); command.add("--height"); command.add(Integer.toString(height)); }

        return new ProcessBuilder(command).directory(instance.toFile()).redirectErrorStream(true).start();
    }

    private void addLibraries(Set<String> out, JsonObject version) {
        if (!version.has("libraries")) return;
        for (JsonElement element : version.getAsJsonArray("libraries")) {
            JsonObject library = element.getAsJsonObject();
            if (library.has("downloads") && library.getAsJsonObject("downloads").has("artifact")) {
                JsonObject artifact = library.getAsJsonObject("downloads").getAsJsonObject("artifact");
                if (artifact.has("path")) out.add(paths.libraries.resolve(artifact.get("path").getAsString().replace('/', java.io.File.separatorChar)).toString());
            }
        }
    }

    private void appendArguments(List<String> command, JsonArray arguments, Map<String, String> values) {
        for (JsonElement element : arguments) {
            if (element.isJsonPrimitive()) {
                String value = element.getAsString();
                for (var entry : values.entrySet()) value = value.replace("${" + entry.getKey() + "}", entry.getValue());
                if (!value.contains("${") ) command.add(value);
            }
        }
    }
}
