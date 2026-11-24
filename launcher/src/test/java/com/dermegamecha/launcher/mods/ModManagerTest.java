package com.dermegamecha.launcher.mods;

import com.dermegamecha.launcher.core.LauncherPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ModManagerTest {
    @Test
    void fabricCatalogExcludesPraxicCardboardAndIncompatibleCreateProjects() {
        var source = "architectury-api clumps emi entityculling fabric-api ferrite-core lithium sable sodium sodium-extra veil";
        assertFalse(source.contains("praxic"));
        assertFalse(source.contains("cardboard"));
        assertFalse(source.contains("create-fabric"));
        assertFalse(source.contains("aeronautics"));
    }
    @Test
    void detectsDuplicateJarsAndDoesNotDeleteUserFiles(@TempDir Path temp) throws Exception {
        Files.createDirectories(temp.resolve("mods"));
        Files.writeString(temp.resolve("mods/sodium-1.2.jar"), "a");
        Files.writeString(temp.resolve("mods/sodium-1.3.jar"), "b");
        var manager = new ModManager(new LauncherPaths());
        var duplicates = manager.detectDuplicates(temp);
        assertFalse(duplicates.isEmpty());
        assertTrue(Files.exists(temp.resolve("mods/sodium-1.2.jar")));
        assertTrue(Files.exists(temp.resolve("mods/sodium-1.3.jar")));
    }
}
