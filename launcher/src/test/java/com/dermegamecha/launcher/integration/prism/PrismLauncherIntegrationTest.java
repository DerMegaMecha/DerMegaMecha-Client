package com.dermegamecha.launcher.integration.prism;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrismLauncherIntegrationTest {
    private final PrismLauncherIntegration prism = new PrismLauncherIntegration();

    @Test
    void detectsPrismInPerUserInstallLocation(@TempDir Path temp) throws Exception {
        Path executable = temp.resolve("Programs").resolve("PrismLauncher").resolve("prismlauncher.exe");
        Files.createDirectories(executable.getParent());
        Files.createFile(executable);

        assertEquals(executable.toAbsolutePath(), prism.detect("", Map.of("LOCALAPPDATA", temp.toString())).orElseThrow());
    }

    @Test
    void reportsMissingAndInvalidExecutables(@TempDir Path temp) throws Exception {
        assertTrue(prism.detect("", Map.of("LOCALAPPDATA", temp.toString())).isEmpty());
        Path wrongFile = temp.resolve("not-prism.exe");
        Files.createFile(wrongFile);
        Path pack = temp.resolve("pack.mrpack");
        Files.createFile(pack);

        assertThrows(IOException.class, () -> prism.importCommand(wrongFile, pack));
        assertThrows(IOException.class, () -> prism.importCommand(temp.resolve("missing.exe"), pack));
    }

    @Test
    void buildsSafeArgumentListsWithoutShellConcatenation(@TempDir Path temp) throws Exception {
        Path executable = temp.resolve("prismlauncher.exe");
        Path pack = temp.resolve("a pack.mrpack");
        Files.createFile(executable);
        Files.createFile(pack);

        assertEquals(java.util.List.of(executable.toString()), prism.openCommand(executable));
        assertEquals(java.util.List.of(executable.toString(), "--import", pack.toString()),
                prism.importCommand(executable, pack));
        assertEquals(java.util.List.of(executable.toString(), "--launch", "My Pack 1",
                        "--server", "play.example.net:25565"),
                prism.launchCommand(executable, "My Pack 1", "play.example.net:25565"));
    }

    @Test
    void rejectsMissingInstanceIdsAndMalformedServerAddresses(@TempDir Path temp) throws Exception {
        Path executable = temp.resolve("prismlauncher.exe");
        Files.createFile(executable);
        assertThrows(IOException.class, () -> prism.launchCommand(executable, "", ""));
        assertThrows(IOException.class, () -> prism.launchCommand(executable, "../other", ""));
        for (String invalid : new String[] {
                "https://example.net", "bad host", "example.net:0", "example.net:65536",
                "example.net; & calc", "[example.net]:25565"
        }) {
            assertThrows(IOException.class, () -> ServerAddressValidator.validate(invalid), invalid);
        }
        assertEquals("play.example.net:25565", ServerAddressValidator.validate("play.example.net:25565"));
        assertEquals("[2001:db8::1]:25565", ServerAddressValidator.validate("[2001:db8::1]:25565"));
    }

    @Test
    void processStartupFailureIsReturnedToCaller(@TempDir Path temp) throws Exception {
        Path executable = temp.resolve("prismlauncher.exe");
        Files.createFile(executable);

        assertThrows(IOException.class, () -> prism.launch(executable, "instance", ""));
    }
}
