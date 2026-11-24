package com.dermegamecha.launcher.mods;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ResourcePackManager {
    public record ResourcePack(String name, Path file, boolean enabled) {}

    public List<ResourcePack> list(Path instance) throws Exception {
        Path directory = instance.resolve("resourcepacks");
        Files.createDirectories(directory);
        List<ResourcePack> result = new ArrayList<>();
        try (var files = Files.list(directory)) {
            files.filter(path -> {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.endsWith(".zip") || name.endsWith(".zip.disabled");
            }).forEach(path -> {
                String name = path.getFileName().toString();
                boolean enabled = name.endsWith(".zip");
                result.add(new ResourcePack(name, path, enabled));
            });
        }
        return List.copyOf(result);
    }

    public void setEnabled(Path instance, String name, boolean enabled) throws Exception {
        Path directory = instance.resolve("resourcepacks");
        Path source = directory.resolve(name);
        if (!name.endsWith(".zip") && !name.endsWith(".zip.disabled")) return;
        Path target = enabled
                ? directory.resolve(name.endsWith(".disabled")
                    ? name.substring(0, name.length() - ".disabled".length()) : name)
                : directory.resolve(name.endsWith(".disabled") ? name : name + ".disabled");
        if (!Files.exists(source) || source.equals(target)) return;
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
